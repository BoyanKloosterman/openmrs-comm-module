package nl.openmrs.comm_module.testgui;

import nl.openmrs.comm_module.testgui.dto.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST voor test-scheduling.html. */
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

    @GetMapping("/appointments")
    public List<PolledAppointmentViewDto> appointments() {
        return testService.listPolledAppointments();
    }

    @GetMapping("/appointments/{appointmentFhirId}")
    public ResponseEntity<PolledAppointmentDetailDto> appointmentDetail(
            @PathVariable String appointmentFhirId) {
        return testService
                .getPolledAppointmentDetail(appointmentFhirId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
