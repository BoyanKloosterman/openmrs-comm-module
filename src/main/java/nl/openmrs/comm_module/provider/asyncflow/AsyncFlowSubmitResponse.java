package nl.openmrs.comm_module.provider.asyncflow;

import java.time.Instant;

public class AsyncFlowSubmitResponse {

    private boolean accepted;
    private String trackingId;
    private String message;
    private Instant submittedAt;

    public AsyncFlowSubmitResponse() {
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}