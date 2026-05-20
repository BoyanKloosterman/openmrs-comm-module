package nl.openmrs.comm_module.testgui;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.OpenmrsFhirAppointmentMetadata;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.AppointmentReminderMessageBuilder;
import nl.openmrs.comm_module.notification.AppointmentReminderPublisher;
import nl.openmrs.comm_module.notification.AppointmentReminderQueryService;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogEntity;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogRepository;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentExclusionEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentExclusionRepository;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import nl.openmrs.comm_module.scheduling.OpenmrsSchedulingFhirSyncService;
import nl.openmrs.comm_module.testgui.dto.*;
import org.hl7.fhir.r5.model.Appointment;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Test-hulp: OpenMRS-patiënten, boeken/annuleren, poll/sync en berichtpreview. */
@Service
public class SchedulingTestService {

    private static final int DELIVERY_LOG_LIMIT = 30;
    private static final int APPOINTMENT_LIST_LIMIT = 50;
    private static final int PATIENT_LIST_LIMIT = 200;
    private static final Duration DEFAULT_OFFSET_AFTER_LEAD = Duration.ofMinutes(5);

    private final Clock clock;
    private final OpenmrsFhirProperties fhirProperties;
    private final NotificationSchedulerProperties schedulerProperties;
    private final OpenmrsSchedulingSyncProperties schedulingSyncProperties;
    private final OpenmrsFhirOperations fhirOperations;
    private final OpenmrsFhirPollingService pollingService;
    private final OpenmrsSchedulingFhirSyncService schedulingSyncService;
    private final OpenmrsSchedulingTestRepository openmrsTestRepository;
    private final AppointmentReminderPublisher appointmentReminderPublisher;
    private final AppointmentReminderQueryService reminderQueryService;
    private final AppointmentReminderMessageBuilder messageBuilder;
    private final NotificationDeliveryLogService deliveryLogService;
    private final PolledAppointmentRepository polledAppointmentRepository;
    private final PolledAppointmentExclusionRepository pollExclusionRepository;
    private final NotificationDeliveryLogRepository deliveryLogRepository;

    public SchedulingTestService(
            Clock clock,
            OpenmrsFhirProperties fhirProperties,
            NotificationSchedulerProperties schedulerProperties,
            OpenmrsSchedulingSyncProperties schedulingSyncProperties,
            OpenmrsFhirOperations fhirOperations,
            OpenmrsFhirPollingService pollingService,
            OpenmrsSchedulingFhirSyncService schedulingSyncService,
            OpenmrsSchedulingTestRepository openmrsTestRepository,
            AppointmentReminderPublisher appointmentReminderPublisher,
            AppointmentReminderQueryService reminderQueryService,
            AppointmentReminderMessageBuilder messageBuilder,
            NotificationDeliveryLogService deliveryLogService,
            PolledAppointmentRepository polledAppointmentRepository,
            PolledAppointmentExclusionRepository pollExclusionRepository,
            NotificationDeliveryLogRepository deliveryLogRepository) {
        this.clock = clock;
        this.fhirProperties = fhirProperties;
        this.schedulerProperties = schedulerProperties;
        this.schedulingSyncProperties = schedulingSyncProperties;
        this.fhirOperations = fhirOperations;
        this.pollingService = pollingService;
        this.schedulingSyncService = schedulingSyncService;
        this.openmrsTestRepository = openmrsTestRepository;
        this.appointmentReminderPublisher = appointmentReminderPublisher;
        this.reminderQueryService = reminderQueryService;
        this.messageBuilder = messageBuilder;
        this.deliveryLogService = deliveryLogService;
        this.polledAppointmentRepository = polledAppointmentRepository;
        this.pollExclusionRepository = pollExclusionRepository;
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
                Arrays.stream(MessagingProviderType.values()).map(Enum::name).toList(),
                window,
                due.size());
    }

    public List<OpenmrsPatientOptionDto> listOpenmrsPatients() {
        return openmrsTestRepository.listPatients(PATIENT_LIST_LIMIT).stream()
                .map(
                        p ->
                                new OpenmrsPatientOptionDto(
                                        p.patientUuid(),
                                        p.displayName(),
                                        maskPhone(p.phone()),
                                        p.phone() != null && !p.phone().isBlank()))
                .toList();
    }

    public List<OpenmrsLocationOptionDto> listOpenmrsLocations() {
        return openmrsTestRepository.listLocations(100).stream()
                .map(l -> new OpenmrsLocationOptionDto(l.locationUuid(), l.name()))
                .toList();
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

    public CreateTestAppointmentResultDto createTestAppointment(
            String patientUuid,
            String locationUuid,
            String reason,
            boolean runSyncAfter,
            boolean runPollAfter,
            String providerName) {
        MessagingProviderType provider = resolveProvider(providerName);
        if (patientUuid == null || patientUuid.isBlank()) {
            throw new IllegalArgumentException("Kies een patiënt uit OpenMRS");
        }
        if (locationUuid == null || locationUuid.isBlank()) {
            throw new IllegalArgumentException("Kies een locatie uit OpenMRS");
        }

        Instant now = clock.instant();
        ZoneId zone = ZoneId.of(schedulingSyncProperties.getZoneId());
        LocalDateTime start = LocalDateTime.ofInstant(computeDefaultAppointmentStart(now), zone);
        String resolvedReason =
                (reason != null && !reason.isBlank())
                        ? reason.trim()
                        : "Testafspraak via comm-module GUI";

        OpenmrsSchedulingTestRepository.BookedOpenmrsAppointment booked =
                openmrsTestRepository.bookAppointment(
                        patientUuid.trim(), locationUuid.trim(), start, resolvedReason);

        String syncNote = null;
        if (runSyncAfter) {
            try {
                int synced = schedulingSyncService.runSync();
                syncNote = "OpenMRS→FHIR sync: " + synced + " geëxporteerd";
            } catch (RuntimeException e) {
                syncNote = "Sync mislukt: " + e.getMessage();
            }
        }

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
        Optional<PolledAppointmentEntity> polled =
                polledAppointmentRepository.findByOrganisationIdAndAppointmentFhirId(
                        fhirProperties.getOrganisationId(), booked.fhirAppointmentId());
        polled.ifPresent(
                p -> {
                    p.setTestMessagingProvider(provider.name());
                    polledAppointmentRepository.save(p);
                });

        return new CreateTestAppointmentResultDto(
                booked.fhirAppointmentId(),
                booked.openmrsAppointmentId(),
                booked.fhirPatientId(),
                start.atZone(zone).toInstant(),
                booked.reason(),
                booked.locationName(),
                booked.patientDisplayName(),
                syncNote,
                pollNote,
                polled.map(p -> toView(p, now, window)).orElse(null));
    }

    @Transactional
    public PolledAppointmentViewDto setTestMessagingProvider(String appointmentFhirId, String providerName) {
        MessagingProviderType provider = resolveProvider(providerName);
        PolledAppointmentEntity entity =
                polledAppointmentRepository
                        .findByOrganisationIdAndAppointmentFhirId(
                                fhirProperties.getOrganisationId(), appointmentFhirId.trim())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Polled appointment niet gevonden: " + appointmentFhirId));
        entity.setTestMessagingProvider(provider.name());
        polledAppointmentRepository.save(entity);
        return toView(entity, clock.instant(), computeWindow(clock.instant()));
    }

    public CancelAppointmentResultDto cancelAppointment(String appointmentFhirId, boolean runSyncAfter, boolean runPollAfter) {
        if (appointmentFhirId == null || appointmentFhirId.isBlank()) {
            throw new IllegalArgumentException("appointmentFhirId ontbreekt");
        }

        Optional<Integer> openmrsId = openmrsTestRepository.resolveOpenmrsAppointmentId(appointmentFhirId);
        boolean cancelled;
        String message;

        if (openmrsId.isPresent()) {
            cancelled = openmrsTestRepository.cancelAppointment(openmrsId.get());
            message =
                    cancelled
                            ? "OpenMRS afspraak geannuleerd (status=CANCELLED)"
                            : "OpenMRS afspraak was al geannuleerd of niet gevonden";
        } else {
            cancelled = cancelFhirOnly(appointmentFhirId);
            message = cancelled ? "FHIR-afspraak op cancelled gezet" : "FHIR-afspraak niet gevonden";
        }

        if (runSyncAfter && openmrsId.isPresent()) {
            try {
                schedulingSyncService.runSync();
            } catch (RuntimeException e) {
                message += "; sync: " + e.getMessage();
            }
        }
        if (runPollAfter) {
            try {
                pollingService.pollOpenmrsFhir();
            } catch (RuntimeException e) {
                message += "; poll: " + e.getMessage();
            }
        }

        return new CancelAppointmentResultDto(
                cancelled, appointmentFhirId, openmrsId.orElse(null), message);
    }

    public TriggerResultDto triggerPoll() {
        try {
            pollingService.pollOpenmrsFhir();
            return new TriggerResultDto(true, "FHIR-poll voltooid");
        } catch (RuntimeException e) {
            return new TriggerResultDto(false, e.getMessage());
        }
    }

    public SchedulerRunResultDto triggerScheduler(String fallbackProviderName) {
        MessagingProviderType fallback = resolveProvider(fallbackProviderName);
        try {
            List<PolledAppointmentEntity> due = reminderQueryService.findAppointmentsDueFor24HourReminder();
            int before = due.size();
            int queued = publishDueWithPerAppointmentProvider(due, fallback);
            int afterListed = reminderQueryService.findAppointmentsDueFor24HourReminder().size();
            return new SchedulerRunResultDto(
                    true,
                    before,
                    afterListed,
                    queued,
                    "per-afspraak",
                    queued + " op queue gezet (provider per afspraak uit test-GUI)");
        } catch (RuntimeException e) {
            return new SchedulerRunResultDto(false, 0, 0, 0, fallback.name(), e.getMessage());
        }
    }

    public SchedulerRunResultDto triggerSchedulerForAppointment(
            String appointmentFhirId, String fallbackProviderName) {
        MessagingProviderType fallback = resolveProvider(fallbackProviderName);
        PolledAppointmentEntity entity =
                polledAppointmentRepository
                        .findByOrganisationIdAndAppointmentFhirId(
                                fhirProperties.getOrganisationId(), appointmentFhirId.trim())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Polled appointment niet gevonden: " + appointmentFhirId));
        MessagingProviderType provider = resolveProviderForEntity(entity, fallback);
        try {
            int queued =
                    appointmentReminderPublisher.publish24HourReminders(List.of(entity), provider);
            return new SchedulerRunResultDto(
                    true,
                    1,
                    0,
                    queued,
                    provider.name(),
                    queued > 0
                            ? "1 bericht via " + provider.name()
                            : "Geen bericht (al verstuurd, geen tel. of niet in venster)");
        } catch (RuntimeException e) {
            return new SchedulerRunResultDto(false, 0, 0, 0, provider.name(), e.getMessage());
        }
    }

    public Optional<MessagePreviewDto> previewMessage(String appointmentFhirId) {
        return polledAppointmentRepository
                .findByOrganisationIdAndAppointmentFhirId(
                        fhirProperties.getOrganisationId(), appointmentFhirId)
                .flatMap(
                        appointment ->
                                buildPreview(
                                        appointment,
                                        resolveProviderForEntity(
                                                appointment, schedulerProperties.getDefaultProvider())));
    }

    private int publishDueWithPerAppointmentProvider(
            List<PolledAppointmentEntity> due, MessagingProviderType fallback) {
        int queued = 0;
        for (PolledAppointmentEntity appointment : due) {
            MessagingProviderType provider = resolveProviderForEntity(appointment, fallback);
            queued +=
                    appointmentReminderPublisher.publish24HourReminders(
                            List.of(appointment), provider);
        }
        return queued;
    }

    public List<DeliveryLogViewDto> listDeliveryLogs() {
        return deliveryLogRepository.findAll().stream()
                .sorted((a, b) -> b.getAttemptedAt().compareTo(a.getAttemptedAt()))
                .limit(DELIVERY_LOG_LIMIT)
                .map(this::toDeliveryLogView)
                .toList();
    }

    @Transactional
    public DeletePolledAppointmentResultDto deletePolledAppointment(
            String appointmentFhirId, boolean clearDeliveryLogs) {
        if (appointmentFhirId == null || appointmentFhirId.isBlank()) {
            throw new IllegalArgumentException("appointmentFhirId ontbreekt");
        }
        PolledAppointmentEntity entity =
                polledAppointmentRepository
                        .findByOrganisationIdAndAppointmentFhirId(
                                fhirProperties.getOrganisationId(), appointmentFhirId.trim())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Polled appointment niet gevonden: " + appointmentFhirId));

        polledAppointmentRepository.delete(entity);
        recordPollExclusion(fhirProperties.getOrganisationId(), appointmentFhirId.trim());
        int logsRemoved = clearDeliveryLogs ? clearDeliveryLogs(appointmentFhirId) : 0;
        return new DeletePolledAppointmentResultDto(
                true,
                appointmentFhirId,
                logsRemoved,
                "Verwijderd uit polled_appointment (poll slaat deze FHIR-id over)"
                        + (logsRemoved > 0 ? " (+ " + logsRemoved + " delivery-log regels)" : ""));
    }

    private void recordPollExclusion(String organisationId, String appointmentFhirId) {
        if (pollExclusionRepository.existsByOrganisationIdAndAppointmentFhirId(
                organisationId, appointmentFhirId)) {
            return;
        }
        PolledAppointmentExclusionEntity exclusion = new PolledAppointmentExclusionEntity();
        exclusion.setOrganisationId(organisationId);
        exclusion.setAppointmentFhirId(appointmentFhirId);
        exclusion.setExcludedAt(clock.instant());
        pollExclusionRepository.save(exclusion);
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

    private boolean cancelFhirOnly(String appointmentFhirId) {
        Optional<Appointment> existing = fhirOperations.readAppointmentByLogicalId(appointmentFhirId);
        if (existing.isEmpty()) {
            return false;
        }
        Appointment appointment = existing.get();
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        fhirOperations.upsertAppointment(appointment);
        return true;
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

    private Optional<MessagePreviewDto> buildPreview(
            PolledAppointmentEntity appointment, MessagingProviderType provider) {
        return messageBuilder.build24HourReminder(appointment).map(msg -> {
            msg.setProvider(provider);
            return toPreview(msg);
        });
    }

    private MessagingProviderType resolveProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return schedulerProperties.getDefaultProvider();
        }
        try {
            return MessagingProviderType.valueOf(providerName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Onbekende provider: " + providerName);
        }
    }

    private MessagingProviderType resolveProviderForEntity(
            PolledAppointmentEntity entity, MessagingProviderType fallback) {
        String stored = entity.getTestMessagingProvider();
        if (stored == null || stored.isBlank()) {
            return fallback;
        }
        try {
            return MessagingProviderType.valueOf(stored.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String findLastDeliveryProvider(String appointmentFhirId) {
        return deliveryLogRepository
                .findFirstByAppointmentFhirIdOrderByAttemptedAtDesc(appointmentFhirId)
                .map(NotificationDeliveryLogEntity::getProvider)
                .orElse(null);
    }

    private PolledAppointmentViewDto toView(
            PolledAppointmentEntity a, Instant now, ReminderWindowDto window) {
        MessagingProviderType previewProvider =
                resolveProviderForEntity(a, schedulerProperties.getDefaultProvider());
        AppointmentWindowStatus status = resolveStatus(a, now, window);
        boolean inWindow = status == AppointmentWindowStatus.IN_REMINDER_WINDOW;
        boolean alreadySent = deliveryLogService.hasSuccessfulDelivery(
                a.getAppointmentFhirId(), AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H);
        Optional<MessagePreviewDto> preview = buildPreview(a, previewProvider);
        Optional<Integer> openmrsId = openmrsTestRepository.resolveOpenmrsAppointmentId(a.getAppointmentFhirId());

        return new PolledAppointmentViewDto(
                a.getAppointmentFhirId(),
                openmrsId.orElse(null),
                openmrsId.isPresent(),
                a.getPatientFhirId(),
                a.getAppointmentDatetime(),
                a.getPatientDisplayName(),
                maskPhone(a.getPatientPhone()),
                a.getLocationId(),
                a.getAppointmentType(),
                a.getAppointmentReason(),
                a.isVoided(),
                a.getLastPolledAt(),
                status,
                inWindow,
                alreadySent,
                a.getTestMessagingProvider(),
                findLastDeliveryProvider(a.getAppointmentFhirId()),
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
