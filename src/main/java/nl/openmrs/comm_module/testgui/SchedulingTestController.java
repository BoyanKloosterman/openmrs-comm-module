package nl.openmrs.comm_module.testgui;

import nl.openmrs.comm_module.testgui.dto.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST voor test-GUI; alleen aan als comm.test-gui.enabled=true. */
@RestController
@RequestMapping("/api/test/scheduling")
@ConditionalOnProperty(name = "comm.test-gui.enabled", havingValue = "true")
public class SchedulingTestController {

    private final SchedulingTestService testService;

    public SchedulingTestController(SchedulingTestService testService) {
        this.testService = testService;
    }

    @GetMapping("/status")
    public SchedulingTestStatusDto status() {
        return testService.status();
    }

    @GetMapping("/patients")
    public List<OpenmrsPatientOptionDto> patients() {
        return testService.listOpenmrsPatients();
    }

    @GetMapping("/locations")
    public List<OpenmrsLocationOptionDto> locations() {
        return testService.listOpenmrsLocations();
    }

    @GetMapping("/appointments")
    public List<PolledAppointmentViewDto> appointments() {
        return testService.listPolledAppointments();
    }

    @GetMapping("/openmrs-appointments")
    public List<OpenmrsAppointmentViewDto> openmrsAppointments(
            @RequestParam(defaultValue = "false") boolean includeVoided) {
        return testService.listOpenmrsAppointments(includeVoided);
    }

    @PutMapping("/openmrs-appointments/{openmrsAppointmentId}")
    public MutateOpenmrsAppointmentResultDto updateOpenmrsAppointment(
            @PathVariable int openmrsAppointmentId,
            @RequestBody(required = false) UpdateOpenmrsAppointmentRequest body) {
        return testService.updateOpenmrsAppointment(openmrsAppointmentId, body);
    }

    @DeleteMapping("/openmrs-appointments/{openmrsAppointmentId}")
    public MutateOpenmrsAppointmentResultDto deleteOpenmrsAppointment(
            @PathVariable int openmrsAppointmentId,
            @RequestParam(defaultValue = "true") boolean runSyncAfter,
            @RequestParam(defaultValue = "true") boolean runPollAfter) {
        return testService.deleteOpenmrsAppointment(openmrsAppointmentId, runSyncAfter, runPollAfter);
    }

    @PutMapping("/appointments/{appointmentFhirId}/test-provider")
    public PolledAppointmentViewDto setTestProvider(
            @PathVariable String appointmentFhirId, @RequestParam String provider) {
        return testService.setTestMessagingProvider(appointmentFhirId, provider);
    }

    @PostMapping("/appointments")
    public CreateTestAppointmentResultDto createAppointment(
            @RequestBody(required = false) CreateTestAppointmentRequest body,
            @RequestParam(required = false) String provider) {
        String patientUuid = body != null ? body.patientUuid() : null;
        String locationUuid = body != null ? body.locationUuid() : null;
        String reason = body != null ? body.reason() : null;
        boolean runSync = body == null || body.runSyncAfter() == null || body.runSyncAfter();
        boolean runPoll = body == null || body.runPollAfter() == null || body.runPollAfter();
        Integer leadHours = body != null ? body.leadHours() : null;
        return testService.createTestAppointment(
                patientUuid, locationUuid, reason, runSync, runPoll, leadHours, provider);
    }

    @PostMapping("/appointments/{appointmentFhirId}/cancel")
    public CancelAppointmentResultDto cancelAppointment(
            @PathVariable String appointmentFhirId,
            @RequestParam(defaultValue = "true") boolean runSyncAfter,
            @RequestParam(defaultValue = "true") boolean runPollAfter) {
        return testService.cancelAppointment(appointmentFhirId, runSyncAfter, runPollAfter);
    }

    @GetMapping("/poll-diagnostics")
    public ResponseEntity<PollDiagnosticsDto> pollDiagnostics() {
        return testService
                .lastPollDiagnostics()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/poll")
    public TriggerResultDto poll() {
        return testService.triggerPoll();
    }

    @PostMapping("/scheduler")
    public SchedulerRunResultDto scheduler(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String reminder) {
        return testService.triggerScheduler(provider, ReminderKind.parse(reminder));
    }

    @PostMapping("/appointments/{appointmentFhirId}/scheduler")
    public SchedulerRunResultDto schedulerForAppointment(
            @PathVariable String appointmentFhirId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String reminder) {
        return testService.triggerSchedulerForAppointment(
                appointmentFhirId, provider, ReminderKind.parse(reminder));
    }

    @GetMapping("/appointments/{appointmentFhirId}/message-preview")
    public ResponseEntity<MessagePreviewDto> messagePreview(
            @PathVariable String appointmentFhirId,
            @RequestParam(required = false) String reminder) {
        return testService
                .previewMessage(appointmentFhirId, ReminderKind.parse(reminder))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/delivery-logs")
    public List<DeliveryLogViewDto> deliveryLogs() {
        return testService.listDeliveryLogs();
    }

    @DeleteMapping("/delivery-logs")
    public ResponseEntity<String> clearDeliveryLogs(
            @RequestParam(required = false) String appointmentFhirId) {
        int removed = testService.clearDeliveryLogs(appointmentFhirId);
        return ResponseEntity.ok(removed + " logregel(s) verwijderd");
    }

    @DeleteMapping("/polled-appointments/{appointmentFhirId}")
    public DeletePolledAppointmentResultDto deletePolledAppointment(
            @PathVariable String appointmentFhirId,
            @RequestParam(defaultValue = "true") boolean clearDeliveryLogs) {
        return testService.deletePolledAppointment(appointmentFhirId, clearDeliveryLogs);
    }
}
