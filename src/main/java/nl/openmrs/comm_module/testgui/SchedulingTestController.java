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

    @GetMapping("/appointments")
    public List<PolledAppointmentViewDto> appointments() {
        return testService.listPolledAppointments();
    }

    @PostMapping("/appointments")
    public CreateTestAppointmentResultDto createAppointment(
            @RequestBody(required = false) CreateTestAppointmentRequest body) {
        String phone = body != null ? body.phone() : null;
        String name = body != null ? body.patientName() : null;
        boolean runPoll = body == null || body.runPollAfter() == null || body.runPollAfter();
        return testService.createTestAppointment(phone, name, runPoll);
    }

    @PostMapping("/poll")
    public TriggerResultDto poll() {
        return testService.triggerPoll();
    }

    @PostMapping("/scheduler")
    public SchedulerRunResultDto scheduler() {
        return testService.triggerScheduler();
    }

    @GetMapping("/appointments/{appointmentFhirId}/message-preview")
    public ResponseEntity<MessagePreviewDto> messagePreview(@PathVariable String appointmentFhirId) {
        return testService
                .previewMessage(appointmentFhirId)
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
}
