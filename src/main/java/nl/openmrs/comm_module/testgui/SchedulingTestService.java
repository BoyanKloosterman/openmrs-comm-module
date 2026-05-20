package nl.openmrs.comm_module.testgui;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.OpenmrsFhirAppointmentMetadata;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.AppointmentReminderMessageBuilder;
import nl.openmrs.comm_module.notification.AppointmentReminderQueryService;
import nl.openmrs.comm_module.notification.DueNotificationProcessor;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogEntity;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogRepository;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import nl.openmrs.comm_module.testgui.dto.*;
import org.hl7.fhir.r5.model.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Test-hulp: vensterstatus, FHIR-afspraak aanmaken, poll/scheduler en berichtpreview. */
@Service
public class SchedulingTestService {

    private static final int DELIVERY_LOG_LIMIT = 30;
    private static final int APPOINTMENT_LIST_LIMIT = 50;
    /** Iets na het midden van het herinneringsvenster (zie docker-scheduling-test.md). */
    private static final Duration DEFAULT_OFFSET_AFTER_LEAD = Duration.ofMinutes(5);

    private final Clock clock;
    private final OpenmrsFhirProperties fhirProperties;
    private final NotificationSchedulerProperties schedulerProperties;
    private final OpenmrsFhirOperations fhirOperations;
    private final OpenmrsFhirPollingService pollingService;
    private final DueNotificationProcessor dueNotificationProcessor;
    private final AppointmentReminderQueryService reminderQueryService;
    private final AppointmentReminderMessageBuilder messageBuilder;
    private final NotificationDeliveryLogService deliveryLogService;
    private final PolledAppointmentRepository polledAppointmentRepository;
    private final NotificationDeliveryLogRepository deliveryLogRepository;

    public SchedulingTestService(
            Clock clock,
            OpenmrsFhirProperties fhirProperties,
            NotificationSchedulerProperties schedulerProperties,
            OpenmrsFhirOperations fhirOperations,
            OpenmrsFhirPollingService pollingService,
            DueNotificationProcessor dueNotificationProcessor,
            AppointmentReminderQueryService reminderQueryService,
            AppointmentReminderMessageBuilder messageBuilder,
            NotificationDeliveryLogService deliveryLogService,
            PolledAppointmentRepository polledAppointmentRepository,
            NotificationDeliveryLogRepository deliveryLogRepository) {
        this.clock = clock;
        this.fhirProperties = fhirProperties;
        this.schedulerProperties = schedulerProperties;
        this.fhirOperations = fhirOperations;
        this.pollingService = pollingService;
        this.dueNotificationProcessor = dueNotificationProcessor;
        this.reminderQueryService = reminderQueryService;
        this.messageBuilder = messageBuilder;
        this.deliveryLogService = deliveryLogService;
        this.polledAppointmentRepository = polledAppointmentRepository;
        this.deliveryLogRepository = deliveryLogRepository;
    }

    public SchedulingTestStatusDto status() {
        Instant now = clock.instant();
        ReminderWindowDto window = computeWindow(now);
        List<PolledAppointmentEntity> due = reminderQueryService.findAppointmentsDueFor24HourReminder();
        return new SchedulingTestStatusDto(
                now,
                fhirProperties.getOrganisationId(),
                schedulerProperties.isEnabled(),
                schedulerProperties.getReminderLeadHours(),
                schedulerProperties.getReminderWindowMinutes(),
                schedulerProperties.getCheckIntervalMinutes(),
                fhirProperties.getPollIntervalMinutes(),
                schedulerProperties.getReminderZoneId(),
                schedulerProperties.getDefaultProvider().name(),
                window,
                due.size());
    }

    public List<PolledAppointmentViewDto> listPolledAppointments() {
        Instant now = clock.instant();
        ReminderWindowDto window = computeWindow(now);
        String orgId = fhirProperties.getOrganisationId();

        return polledAppointmentRepository
                .findAll(PageRequest.of(0, APPOINTMENT_LIST_LIMIT))
                .getContent()
                .stream()
                .filter(a -> orgId.equals(a.getOrganisationId()))
                .sorted((a, b) -> b.getLastPolledAt().compareTo(a.getLastPolledAt()))
                .map(a -> toView(a, now, window))
                .toList();
    }

    public CreateTestAppointmentResultDto createTestAppointment(String phone, String patientName, boolean runPollAfter) {
        Instant now = clock.instant();
        Instant start = computeDefaultAppointmentStart(now);
        String suffix = Long.toHexString(System.currentTimeMillis());
        String patientId = "patient-gui-" + suffix;
        String appointmentId = "apt-gui-" + suffix;

        String resolvedPhone = (phone != null && !phone.isBlank()) ? phone.trim() : "+31612345678";
        String resolvedName = (patientName != null && !patientName.isBlank()) ? patientName.trim() : "Test Patiënt GUI";

        fhirOperations.upsertPatient(buildPatient(patientId, resolvedName, resolvedPhone));
        fhirOperations.upsertAppointment(buildAppointment(appointmentId, patientId, start));

        String pollNote = null;
        if (runPollAfter) {
            try {
                pollingService.pollOpenmrsFhir();
                pollNote = "FHIR-poll uitgevoerd";
            } catch (RuntimeException e) {
                pollNote = "Poll mislukt: " + e.getMessage();
            }
        }

        ReminderWindowDto window = computeWindow(now);
        Optional<PolledAppointmentEntity> polled = polledAppointmentRepository.findByOrganisationIdAndAppointmentFhirId(
                fhirProperties.getOrganisationId(), appointmentId);

        return new CreateTestAppointmentResultDto(
                appointmentId,
                patientId,
                start,
                resolvedPhone,
                resolvedName,
                pollNote,
                polled.map(p -> toView(p, now, window)).orElse(null));
    }

    public TriggerResultDto triggerPoll() {
        try {
            pollingService.pollOpenmrsFhir();
            return new TriggerResultDto(true, "FHIR-poll voltooid");
        } catch (RuntimeException e) {
            return new TriggerResultDto(false, e.getMessage());
        }
    }

    public SchedulerRunResultDto triggerScheduler() {
        try {
            int before = reminderQueryService.findAppointmentsDueFor24HourReminder().size();
            dueNotificationProcessor.processDueNotifications();
            int afterListed = reminderQueryService.findAppointmentsDueFor24HourReminder().size();
            return new SchedulerRunResultDto(true, before, afterListed, "Scheduler-tick uitgevoerd");
        } catch (RuntimeException e) {
            return new SchedulerRunResultDto(false, 0, 0, e.getMessage());
        }
    }

    public Optional<MessagePreviewDto> previewMessage(String appointmentFhirId) {
        return polledAppointmentRepository
                .findByOrganisationIdAndAppointmentFhirId(fhirProperties.getOrganisationId(), appointmentFhirId)
                .flatMap(appointment ->
                        messageBuilder.build24HourReminder(appointment).map(this::toPreview));
    }

    public List<DeliveryLogViewDto> listDeliveryLogs() {
        return deliveryLogRepository.findAll().stream()
                .sorted((a, b) -> b.getAttemptedAt().compareTo(a.getAttemptedAt()))
                .limit(DELIVERY_LOG_LIMIT)
                .map(this::toDeliveryLogView)
                .toList();
    }

    @Transactional
    public int clearDeliveryLogs(String appointmentFhirId) {
        List<NotificationDeliveryLogEntity> all = deliveryLogRepository.findAll();
        List<NotificationDeliveryLogEntity> toDelete = new ArrayList<>();
        for (NotificationDeliveryLogEntity entry : all) {
            if (appointmentFhirId == null
                    || appointmentFhirId.isBlank()
                    || appointmentFhirId.equals(entry.getAppointmentFhirId())) {
                toDelete.add(entry);
            }
        }
        deliveryLogRepository.deleteAll(toDelete);
        return toDelete.size();
    }

    private Instant computeDefaultAppointmentStart(Instant now) {
        int leadHours = Math.max(0, schedulerProperties.getReminderLeadHours());
        return now.plus(Duration.ofHours(leadHours)).plus(DEFAULT_OFFSET_AFTER_LEAD);
    }

    private ReminderWindowDto computeWindow(Instant now) {
        int leadHours = Math.max(0, schedulerProperties.getReminderLeadHours());
        int windowMinutes = Math.max(1, schedulerProperties.getReminderWindowMinutes());
        Duration halfWindow = Duration.ofMinutes(windowMinutes / 2L);

        Instant target = now.plus(Duration.ofHours(leadHours));
        Instant windowStart = target.minus(halfWindow);
        Instant windowEnd = target.plus(halfWindow);
        return new ReminderWindowDto(target, windowStart, windowEnd, leadHours, windowMinutes);
    }

    private PolledAppointmentViewDto toView(
            PolledAppointmentEntity a, Instant now, ReminderWindowDto window) {
        AppointmentWindowStatus status = resolveStatus(a, now, window);
        boolean inWindow = status == AppointmentWindowStatus.IN_REMINDER_WINDOW;
        boolean alreadySent = deliveryLogService.hasSuccessfulDelivery(
                a.getAppointmentFhirId(), AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H);
        Optional<MessagePreviewDto> preview =
                messageBuilder.build24HourReminder(a).map(this::toPreview);

        return new PolledAppointmentViewDto(
                a.getAppointmentFhirId(),
                a.getPatientFhirId(),
                a.getAppointmentDatetime(),
                a.getPatientDisplayName(),
                maskPhone(a.getPatientPhone()),
                a.getLocationId(),
                a.getAppointmentType(),
                a.isVoided(),
                a.getLastPolledAt(),
                status,
                inWindow,
                alreadySent,
                preview.orElse(null));
    }

    private AppointmentWindowStatus resolveStatus(
            PolledAppointmentEntity a, Instant now, ReminderWindowDto window) {
        if (a.isVoided()) {
            return AppointmentWindowStatus.VOIDED;
        }
        Instant start = a.getAppointmentDatetime();
        if (start == null || !start.isAfter(now)) {
            return AppointmentWindowStatus.APPOINTMENT_PAST;
        }
        if (a.getPatientPhone() == null || a.getPatientPhone().isBlank()) {
            return AppointmentWindowStatus.MISSING_PHONE;
        }
        if (start.isBefore(window.windowStart())) {
            return AppointmentWindowStatus.TOO_EARLY;
        }
        if (!start.isBefore(window.windowEnd())) {
            return AppointmentWindowStatus.TOO_LATE;
        }
        return AppointmentWindowStatus.IN_REMINDER_WINDOW;
    }

    private static Patient buildPatient(String id, String displayName, String phone) {
        Patient patient = new Patient();
        patient.setId(id);
        HumanName name = new HumanName();
        String[] parts = displayName.split("\\s+", 2);
        if (parts.length > 0 && !parts[0].isBlank()) {
            name.addGiven(parts[0].trim());
        }
        if (parts.length > 1 && !parts[1].isBlank()) {
            name.setFamily(parts[1].trim());
        } else if (parts.length == 1) {
            name.setFamily(parts[0].trim());
        }
        patient.addName(name);
        patient.addTelecom(
                new ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(phone));
        return patient;
    }

    private static Appointment buildAppointment(String id, String patientId, Instant start) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
        appointment.setStart(Date.from(start));
        appointment.setEnd(Date.from(start.plus(Duration.ofMinutes(30))));
        appointment.setSubject(new Reference("Patient/" + patientId));
        appointment.addServiceType(
                new CodeableReference(new CodeableConcept().setText("GUI-test consult")));
        OpenmrsFhirAppointmentMetadata.applyTo(
                appointment, "Test Polikliniek B12", "Kom 10 minuten van tevoren (GUI-test)");
        Appointment.AppointmentParticipantComponent participant =
                new Appointment.AppointmentParticipantComponent();
        participant.setActor(new Reference("Patient/" + patientId));
        participant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
        appointment.addParticipant(participant);
        return appointment;
    }

    private MessagePreviewDto toPreview(NotificationQueueMessage message) {
        return new MessagePreviewDto(
                message.getNotificationId(),
                message.getRecipient(),
                message.getSubject(),
                message.getBody(),
                message.getProvider() != null ? message.getProvider().name() : null,
                message.getMessageType(),
                message.getAppointmentFhirId());
    }

    private DeliveryLogViewDto toDeliveryLogView(NotificationDeliveryLogEntity e) {
        return new DeliveryLogViewDto(
                e.getId(),
                e.getNotificationId(),
                e.getAppointmentFhirId(),
                e.getMessageType(),
                e.getProvider(),
                e.getStatus(),
                e.isSuccessful(),
                e.getProviderMessageId(),
                e.getErrorMessage(),
                e.getAttemptedAt());
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return "***" + phone.substring(phone.length() - 4);
    }
}
