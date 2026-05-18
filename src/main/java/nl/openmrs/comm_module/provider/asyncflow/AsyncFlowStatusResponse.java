package nl.openmrs.comm_module.provider.asyncflow;

import java.time.Instant;

public class AsyncFlowStatusResponse {

    private String trackingId;
    private String status;
    private Instant submittedAt;
    private Instant processedAt;
    private String errorDetails;

    public AsyncFlowStatusResponse() {
    }

    public String getTrackingId() {
        return trackingId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getErrorDetails() {
        return errorDetails;
    }
}