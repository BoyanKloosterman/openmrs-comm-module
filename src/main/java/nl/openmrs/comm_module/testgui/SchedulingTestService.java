package nl.openmrs.comm_module.testgui;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.config.OpenmrsDataSourceProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.OpenmrsFhirAppointmentMetadata;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.AppointmentReminderMessageBuilder;
import nl.openmrs.comm_module.notification.AppointmentReminderPublisher;
import nl.openmrs.comm_module.notification.AppointmentReminderQueryService;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderCatalog;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderConfiguration;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderSpec;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogEntity;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogRepository;
import nl.openmrs.comm_module.poll.AppointmentPollExclusionService;
import nl.openmrs.comm_module.poll.PollDiagnosticsRecorder;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.source.AppointmentPollWindow;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import nl.openmrs.comm_module.scheduling.OpenmrsSchedulingFhirSyncService;
import nl.openmrs.comm_module.testgui.dto.*;
import org.hl7.fhir.r5.model.Appointment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Test-hulp: OpenMRS-patiënten, boeken/annuleren, poll/sync en berichtpreview. */
@Service
public class SchedulingTestService {

    private static final int DELIVERY_LOG_LIMIT = 30;
    private static final int APPOINTMENT_LIST_LIMIT = 50;
    private static final int OPENMRS_APPOINTMENT_LIST_LIMIT = 200;
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
    private final AppointmentReminderCatalog reminderCatalog;
    private final AppointmentReminderPublisher appointmentReminderPublisher;
    private final AppointmentReminderQueryService reminderQueryService;
    private final AppointmentReminderMessageBuilder messageBuilder;
    private final NotificationDeliveryLogService deliveryLogService;
    private final PolledAppointmentRepository polledAppointmentRepository;
    private final AppointmentPollExclusionService pollExclusionService;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final PollDiagnosticsRecorder pollDiagnosticsRecorder;
    private final OpenmrsDataSourceProperties dataSourceProperties;
    private final String pollMode;

    public SchedulingTestService(
            Clock clock,
            OpenmrsFhirProperties fhirProperties,
            NotificationSchedulerProperties schedulerProperties,
            OpenmrsSchedulingSyncProperties schedulingSyncProperties,
            OpenmrsFhirOperations fhirOperations,
            OpenmrsFhirPollingService pollingService,
            OpenmrsSchedulingFhirSyncService schedulingSyncService,
            OpenmrsSchedulingTestRepository openmrsTestRepository,
            AppointmentReminderCatalog reminderCatalog,
            AppointmentReminderPublisher appointmentReminderPublisher,
            AppointmentReminderQueryService reminderQueryService,
            AppointmentReminderMessageBuilder messageBuilder,
            NotificationDeliveryLogService deliveryLogService,
            PolledAppointmentRepository polledAppointmentRepository,
            AppointmentPollExclusionService pollExclusionService,
            NotificationDeliveryLogRepository deliveryLogRepository,
            PollDiagnosticsRecorder pollDiagnosticsRecorder,
            OpenmrsDataSourceProperties dataSourceProperties,
            @Value("${openmrs.fhir.poll-mode:fhir}") String pollMode) {
        this.clock = clock;
        this.fhirProperties = fhirProperties;
        this.schedulerProperties = schedulerProperties;
        this.schedulingSyncProperties = schedulingSyncProperties;
        this.fhirOperations = fhirOperations;
        this.pollingService = pollingService;
        this.schedulingSyncService = schedulingSyncService;
        this.openmrsTestRepository = openmrsTestRepository;
        this.reminderCatalog = reminderCatalog;
        this.appointmentReminderPublisher = appointmentReminderPublisher;
        this.reminderQueryService = reminderQueryService;
        this.messageBuilder = messageBuilder;
        this.deliveryLogService = deliveryLogService;
        this.polledAppointmentRepository = polledAppointmentRepository;
        this.pollExclusionService = pollExclusionService;
        this.deliveryLogRepository = deliveryLogRepository;
        this.pollDiagnosticsRecorder = pollDiagnosticsRecorder;
        this.dataSourceProperties = dataSourceProperties;
        this.pollMode = pollMode;
    }

    public SchedulingTestStatusDto status() {
        Instant now = clock.instant();
        ReminderWindowDto window24 = computeWindow(now, schedulerProperties.getReminderLeadHours());
        ReminderWindowDto window1 = computeWindow(now, schedulerProperties.getReminder1LeadHours());
        AppointmentReminderSpec spec24 = requireSpec(ReminderKind.H24);
        AppointmentReminderSpec spec1 = requireSpec(ReminderKind.H1);
        List<PolledAppointmentEntity> due24 = reminderQueryService.findAppointmentsDueFor(spec24);
        List<PolledAppointmentEntity> due1 = reminderQueryService.findAppointmentsDueFor(spec1);
        Instant pollFrom = AppointmentPollWindow.from(now, fhirProperties);
        Instant pollTo = AppointmentPollWindow.to(now);
        return new SchedulingTestStatusDto(
                now,
                fhirProperties.getOrganisationId(),
                resolvePollSourceLabel(),
                schedulingSyncProperties.isEnabled(),
                fhirProperties.getAppointmentPollSinceDays(),
                pollFrom,
                pollTo,
                schedulerProperties.isEnabled(),
                schedulerProperties.getReminderLeadHours(),
                schedulerProperties.getReminder1LeadHours(),
                schedulerProperties.getReminderWindowMinutes(),
                schedulerProperties.getCheckIntervalMinutes(),
                fhirProperties.getPollIntervalMinutes(),
                schedulerProperties.getReminderZoneId(),
                schedulerProperties.getDefaultProvider().name(),
                Arrays.stream(MessagingProviderType.values()).map(Enum::name).toList(),
                window24,
                due24.size(),
                window1,
                due1.size(),
                pollDiagnosticsRecorder.getLast().orElse(null));
    }

    public Optional<PollDiagnosticsDto> lastPollDiagnostics() {
        return pollDiagnosticsRecorder.getLast();
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

    public List<OpenmrsAppointmentViewDto> listOpenmrsAppointments(boolean includeVoided) {
        return openmrsTestRepository.listAppointments(OPENMRS_APPOINTMENT_LIST_LIMIT, includeVoided).stream()
                .map(this::toOpenmrsAppointmentView)
                .toList();
    }

    public MutateOpenmrsAppointmentResultDto updateOpenmrsAppointment(
            int openmrsAppointmentId, UpdateOpenmrsAppointmentRequest body) {
        if (body == null) {
            throw new IllegalArgumentException("Body ontbreekt");
        }
        boolean runSync = body.runSyncAfter() == null || body.runSyncAfter();
        boolean runPoll = body.runPollAfter() == null || body.runPollAfter();
        LocalDateTime start = parseAppointmentStartUtc(body.appointmentStart());

        boolean updated =
                openmrsTestRepository.updateAppointment(
                        openmrsAppointmentId, body.reason(), body.status(), start, body.locationUuid());
        String fhirId = "omrs-appt-" + openmrsAppointmentId;
        if (!updated) {
            return new MutateOpenmrsAppointmentResultDto(
                    false, openmrsAppointmentId, fhirId, "Afspraak niet bijgewerkt (niet gevonden of voided)");
        }
        String message = runSyncPollAfterMutate(runSync, runPoll, "Afspraak bijgewerkt in OpenMRS");
        return new MutateOpenmrsAppointmentResultDto(true, openmrsAppointmentId, fhirId, message);
    }

    public MutateOpenmrsAppointmentResultDto deleteOpenmrsAppointment(
            int openmrsAppointmentId, boolean runSyncAfter, boolean runPollAfter) {
        boolean deleted = openmrsTestRepository.voidAppointment(openmrsAppointmentId);
        String fhirId = "omrs-appt-" + openmrsAppointmentId;
        if (!deleted) {
            return new MutateOpenmrsAppointmentResultDto(
                    false, openmrsAppointmentId, fhirId, "Afspraak niet verwijderd (niet gevonden of al voided)");
        }
        String message = runSyncPollAfterMutate(runSyncAfter, runPollAfter, "Afspraak verwijderd (voided) in OpenMRS");
        return new MutateOpenmrsAppointmentResultDto(true, openmrsAppointmentId, fhirId, message);
    }

    public List<PolledAppointmentViewDto> listPolledAppointments() {
        Instant now = clock.instant();
        ReminderWindowDto window24 = computeWindow(now, schedulerProperties.getReminderLeadHours());
        ReminderWindowDto window1 = computeWindow(now, schedulerProperties.getReminder1LeadHours());
        String orgId = fhirProperties.getOrganisationId();

        return polledAppointmentRepository
                .findAll(PageRequest.of(0, APPOINTMENT_LIST_LIMIT))
                .getContent()
                .stream()
                .filter(a -> orgId.equals(a.getOrganisationId()))
                .sorted((a, b) -> b.getLastPolledAt().compareTo(a.getLastPolledAt()))
                .map(a -> toView(a, now, window24, window1))
                .toList();
    }

    public CreateTestAppointmentResultDto createTestAppointment(
            String patientUuid,
            String locationUuid,
            String reason,
            boolean runSyncAfter,
            boolean runPollAfter,
            Integer leadHours,
            String providerName) {
        MessagingProviderType provider = resolveProvider(providerName);
        if (patientUuid == null || patientUuid.isBlank()) {
            throw new IllegalArgumentException("Kies een patiënt uit OpenMRS");
        }
        if (locationUuid == null || locationUuid.isBlank()) {
            throw new IllegalArgumentException("Kies een locatie uit OpenMRS");
        }

        Instant now = clock.instant();
        int resolvedLead = resolveLeadHours(leadHours);
        ZoneId zone = ZoneId.of(schedulingSyncProperties.getZoneId());
        LocalDateTime start = LocalDateTime.ofInstant(computeDefaultAppointmentStart(now, resolvedLead), zone);
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

        ReminderWindowDto window24 = computeWindow(now, schedulerProperties.getReminderLeadHours());
        ReminderWindowDto window1 = computeWindow(now, schedulerProperties.getReminder1LeadHours());
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
                polled.map(p -> toView(p, now, window24, window1)).orElse(null));
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
        Instant now = clock.instant();
        return toView(
                entity,
                now,
                computeWindow(now, schedulerProperties.getReminderLeadHours()),
                computeWindow(now, schedulerProperties.getReminder1LeadHours()));
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
            return buildPollResult();
        } catch (RuntimeException e) {
            return new TriggerResultDto(false, e.getMessage(), pollDiagnosticsRecorder.getLast().orElse(null));
        }
    }

    private TriggerResultDto buildPollResult() {
        PollDiagnosticsDto diagnostics = pollDiagnosticsRecorder.getLast().orElse(null);
        if (diagnostics == null) {
            return new TriggerResultDto(false, "Geen FHIR-bron geconfigureerd (openmrs.fhir.server-url)", null);
        }
        String message = diagnostics.summary();
        return new TriggerResultDto(diagnostics.success(), message, diagnostics);
    }

    public SchedulerRunResultDto triggerScheduler(String fallbackProviderName, ReminderKind kind) {
        MessagingProviderType fallback = resolveProvider(fallbackProviderName);
        ReminderKind resolved = kind != null ? kind : ReminderKind.ALL;
        try {
            int before = countDue(resolved);
            int queued = publishDueWithPerAppointmentProvider(resolved, fallback);
            int afterListed = countDue(resolved);
            return new SchedulerRunResultDto(
                    true,
                    before,
                    afterListed,
                    queued,
                    "per-afspraak",
                    queued + " op queue (" + resolved + ", provider per afspraak)");
        } catch (RuntimeException e) {
            return new SchedulerRunResultDto(false, 0, 0, 0, fallback.name(), e.getMessage());
        }
    }

    public SchedulerRunResultDto triggerSchedulerForAppointment(
            String appointmentFhirId, String fallbackProviderName, ReminderKind kind) {
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
        ReminderKind resolved = kind != null ? kind : ReminderKind.ALL;
        try {
            int queued = publishForEntity(entity, provider, resolved);
            return new SchedulerRunResultDto(
                    true,
                    1,
                    0,
                    queued,
                    provider.name(),
                    queued > 0
                            ? queued + " bericht(en) via " + provider.name() + " (" + resolved + ")"
                            : "Geen bericht (al verstuurd, geen tel. of niet in venster)");
        } catch (RuntimeException e) {
            return new SchedulerRunResultDto(false, 0, 0, 0, provider.name(), e.getMessage());
        }
    }

    public Optional<MessagePreviewDto> previewMessage(String appointmentFhirId, ReminderKind kind) {
        ReminderKind resolved = kind != null ? kind : ReminderKind.H24;
        return polledAppointmentRepository
                .findByOrganisationIdAndAppointmentFhirId(
                        fhirProperties.getOrganisationId(), appointmentFhirId)
                .flatMap(
                        appointment ->
                                buildPreview(
                                        appointment,
                                        resolveProviderForEntity(
                                                appointment, schedulerProperties.getDefaultProvider()),
                                        resolved));
    }

    private int countDue(ReminderKind kind) {
        int total = 0;
        for (AppointmentReminderSpec spec : specsFor(kind)) {
            total += reminderQueryService.findAppointmentsDueFor(spec).size();
        }
        return total;
    }

    private int publishDueWithPerAppointmentProvider(ReminderKind kind, MessagingProviderType fallback) {
        int queued = 0;
        for (AppointmentReminderSpec spec : specsFor(kind)) {
            for (PolledAppointmentEntity appointment :
                    reminderQueryService.findAppointmentsDueFor(spec)) {
                MessagingProviderType provider = resolveProviderForEntity(appointment, fallback);
                queued +=
                        appointmentReminderPublisher.publishReminders(
                                List.of(appointment), spec, provider);
            }
        }
        return queued;
    }

    private int publishForEntity(
            PolledAppointmentEntity entity, MessagingProviderType provider, ReminderKind kind) {
        int queued = 0;
        for (AppointmentReminderSpec spec : specsFor(kind)) {
            queued += appointmentReminderPublisher.publishReminders(List.of(entity), spec, provider);
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
        pollExclusionService.excludeIfAbsent(organisationId, appointmentFhirId);
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

    private String runSyncPollAfterMutate(boolean runSyncAfter, boolean runPollAfter, String baseMessage) {
        String message = baseMessage;
        if (runSyncAfter) {
            try {
                schedulingSyncService.runSync();
                message += "; sync voltooid";
            } catch (RuntimeException e) {
                message += "; sync: " + e.getMessage();
            }
        }
        if (runPollAfter) {
            try {
                pollingService.pollOpenmrsFhir();
                message += "; poll voltooid";
            } catch (RuntimeException e) {
                message += "; poll: " + e.getMessage();
            }
        }
        return message;
    }

    private String resolvePollSourceLabel() {
        String url = fhirProperties.getServerUrl();
        if (url != null && !url.isBlank()) {
            return url.trim();
        }
        if ("jdbc".equalsIgnoreCase(pollMode) && dataSourceProperties.isConfigured()) {
            return dataSourceProperties.getUrl().trim();
        }
        return "";
    }

    private static LocalDateTime parseAppointmentStartUtc(String appointmentStart) {
        if (appointmentStart == null || appointmentStart.isBlank()) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.parse(appointmentStart.trim()), ZoneOffset.UTC);
    }

    private OpenmrsAppointmentViewDto toOpenmrsAppointmentView(
            OpenmrsSchedulingTestRepository.OpenmrsAppointmentListRow row) {
        return new OpenmrsAppointmentViewDto(
                row.appointmentId(),
                row.fhirAppointmentId(),
                row.status(),
                row.voided(),
                row.startInstant(),
                row.endInstant(),
                row.patientDisplayName(),
                row.patientUuid(),
                row.locationName(),
                row.locationUuid(),
                row.typeName(),
                row.reason());
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

    private int resolveLeadHours(Integer leadHours) {
        if (leadHours == null) {
            return Math.max(0, schedulerProperties.getReminderLeadHours());
        }
        return Math.max(0, leadHours);
    }

    private Instant computeDefaultAppointmentStart(Instant now, int leadHours) {
        return now.plus(Duration.ofHours(leadHours)).plus(DEFAULT_OFFSET_AFTER_LEAD);
    }

    private ReminderWindowDto computeWindow(Instant now, int leadHours) {
        int resolvedLead = Math.max(0, leadHours);
        int windowMinutes = Math.max(1, schedulerProperties.getReminderWindowMinutes());
        Duration halfWindow = Duration.ofMinutes(windowMinutes / 2L);

        Instant target = now.plus(Duration.ofHours(resolvedLead));
        Instant windowStart = target.minus(halfWindow);
        Instant windowEnd = target.plus(halfWindow);
        return new ReminderWindowDto(target, windowStart, windowEnd, resolvedLead, windowMinutes);
    }

    private Optional<MessagePreviewDto> buildPreview(
            PolledAppointmentEntity appointment, MessagingProviderType provider, ReminderKind kind) {
        return messageBuilder
                .buildReminder(appointment, requireSpec(kind))
                .map(
                msg -> {
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
            PolledAppointmentEntity a, Instant now, ReminderWindowDto window24, ReminderWindowDto window1) {
        MessagingProviderType previewProvider =
                resolveProviderForEntity(a, schedulerProperties.getDefaultProvider());
        AppointmentWindowStatus status24 = resolveStatus(a, now, window24);
        AppointmentWindowStatus status1 = resolveStatus(a, now, window1);
        boolean alreadySent24 = deliveryLogService.hasSuccessfulDelivery(
                a.getAppointmentFhirId(), AppointmentReminderConfiguration.MESSAGE_TYPE_24H);
        boolean alreadySent1 = deliveryLogService.hasSuccessfulDelivery(
                a.getAppointmentFhirId(), AppointmentReminderConfiguration.MESSAGE_TYPE_1H);
        Optional<MessagePreviewDto> preview24 = buildPreview(a, previewProvider, ReminderKind.H24);
        Optional<MessagePreviewDto> preview1 = buildPreview(a, previewProvider, ReminderKind.H1);
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
                status24,
                status24 == AppointmentWindowStatus.IN_REMINDER_WINDOW,
                alreadySent24,
                status1,
                status1 == AppointmentWindowStatus.IN_REMINDER_WINDOW,
                alreadySent1,
                a.getTestMessagingProvider(),
                findLastDeliveryProvider(a.getAppointmentFhirId()),
                preview24.orElse(null),
                preview1.orElse(null));
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

    private AppointmentReminderSpec requireSpec(ReminderKind kind) {
        if (kind == ReminderKind.ALL) {
            throw new IllegalArgumentException("Geen enkele spec voor ReminderKind.ALL");
        }
        String id =
                kind == ReminderKind.H24
                        ? AppointmentReminderConfiguration.ID_24H
                        : AppointmentReminderConfiguration.ID_1H;
        return reminderCatalog
                .findById(id)
                .orElseThrow(() -> new IllegalStateException("Herinneringsspec ontbreekt: " + id));
    }

    private List<AppointmentReminderSpec> specsFor(ReminderKind kind) {
        return switch (kind) {
            case H24 -> List.of(requireSpec(ReminderKind.H24));
            case H1 -> List.of(requireSpec(ReminderKind.H1));
            case ALL -> reminderCatalog.all();
        };
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return "***" + phone.substring(phone.length() - 4);
    }
}
