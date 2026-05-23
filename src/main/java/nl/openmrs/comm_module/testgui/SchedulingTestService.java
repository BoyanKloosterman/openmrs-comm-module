package nl.openmrs.comm_module.testgui;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.config.OpenmrsDataSourceProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.AppointmentReminderMessageBuilder;
import nl.openmrs.comm_module.notification.content.AppointmentNotificationContent;
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
import nl.openmrs.comm_module.testgui.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Test-GUI: status, polled lijst, poll/scheduler triggers, delivery logs. */
@Service
@ConditionalOnProperty(name = "comm.test-gui.enabled", havingValue = "true")
public class SchedulingTestService {

    private static final DateTimeFormatter DETAIL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("nl-NL"));
    private static final DateTimeFormatter DETAIL_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final int DELIVERY_LOG_LIMIT = 30;
    private static final int APPOINTMENT_LIST_LIMIT = 50;

    private final Clock clock;
    private final OpenmrsFhirProperties fhirProperties;
    private final NotificationSchedulerProperties schedulerProperties;
    private final OpenmrsSchedulingSyncProperties schedulingSyncProperties;
    private final OpenmrsFhirPollingService pollingService;
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
            OpenmrsFhirPollingService pollingService,
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
        this.pollingService = pollingService;
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
                schedulingSyncProperties.effectiveDbZoneId().getId(),
                schedulingSyncProperties.getZoneId(),
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

    public Optional<PolledAppointmentDetailDto> getPolledAppointmentDetail(String appointmentFhirId) {
        if (appointmentFhirId == null || appointmentFhirId.isBlank()) {
            return Optional.empty();
        }
        return polledAppointmentRepository
                .findByOrganisationIdAndAppointmentFhirId(
                        fhirProperties.getOrganisationId(), appointmentFhirId.trim())
                .map(this::toDetail);
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
            return new TriggerResultDto(false, "Geen poll-bron (FHIR-url of JDBC datasource)", null);
        }
        return new TriggerResultDto(diagnostics.success(), diagnostics.summary(), diagnostics);
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
        pollExclusionService.excludeIfAbsent(fhirProperties.getOrganisationId(), appointmentFhirId.trim());
        int logsRemoved = clearDeliveryLogs ? clearDeliveryLogs(appointmentFhirId) : 0;
        return new DeletePolledAppointmentResultDto(
                true,
                appointmentFhirId,
                logsRemoved,
                "Verwijderd uit polled_appointment (poll slaat deze FHIR-id over)"
                        + (logsRemoved > 0 ? " (+ " + logsRemoved + " delivery-log regels)" : ""));
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

    private String resolvePollSourceLabel() {
        String fhirUrl = fhirProperties.getServerUrl();
        boolean jdbc = dataSourceProperties.isConfigured();
        if (fhirUrl != null && !fhirUrl.isBlank()) {
            String label = fhirUrl.trim();
            if (jdbc && !"jdbc".equalsIgnoreCase(pollMode)) {
                label += " (+ JDBC-fallback)";
            }
            return label;
        }
        if (jdbc) {
            return dataSourceProperties.getUrl().trim();
        }
        return "";
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

    private PolledAppointmentDetailDto toDetail(PolledAppointmentEntity a) {
        AppointmentNotificationContent content = messageBuilder.resolveContent(a);
        MessagingProviderType previewProvider =
                resolveProviderForEntity(a, schedulerProperties.getDefaultProvider());
        Optional<MessagePreviewDto> preview24 = buildPreview(a, previewProvider, ReminderKind.H24);
        Optional<MessagePreviewDto> preview1 = buildPreview(a, previewProvider, ReminderKind.H1);

        return new PolledAppointmentDetailDto(
                a.getAppointmentFhirId(),
                OpenmrsLegacyAppointmentIds.resolveOpenmrsId(a.getAppointmentFhirId()).orElse(null),
                a.getPatientDisplayName(),
                maskPhone(a.getPatientPhone()),
                a.getAppointmentDatetime(),
                DETAIL_DATE_FORMAT.format(content.appointmentTime()),
                DETAIL_TIME_FORMAT.format(content.appointmentTime()),
                content.locationOrDefault(),
                blankToNull(a.getAppointmentType()),
                content.hasInstructions() ? content.instructions() : null,
                a.isVoided(),
                preview24.orElse(null),
                preview1.orElse(null));
    }

    private PolledAppointmentViewDto toView(
            PolledAppointmentEntity a, Instant now, ReminderWindowDto window24, ReminderWindowDto window1) {
        return new PolledAppointmentViewDto(
                a.getAppointmentFhirId(),
                a.getAppointmentDatetime(),
                a.getPatientDisplayName(),
                maskPhone(a.getPatientPhone()),
                a.isVoided(),
                resolveStatus(a, now, window24),
                deliveryLogService.hasSuccessfulDelivery(
                        a.getAppointmentFhirId(), AppointmentReminderConfiguration.MESSAGE_TYPE_24H),
                resolveStatus(a, now, window1),
                deliveryLogService.hasSuccessfulDelivery(
                        a.getAppointmentFhirId(), AppointmentReminderConfiguration.MESSAGE_TYPE_1H));
    }

    private AppointmentWindowStatus resolveStatus(
            PolledAppointmentEntity a, Instant now, ReminderWindowDto window) {
        return AppointmentReminderWindowStatusResolver.resolve(a, now, window);
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return "***" + phone.substring(phone.length() - 4);
    }
}
