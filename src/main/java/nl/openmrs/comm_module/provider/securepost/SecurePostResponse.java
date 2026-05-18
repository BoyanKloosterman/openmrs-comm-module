package nl.openmrs.comm_module.provider.securepost;

import java.time.Instant;

public class SecurePostResponse {

    private boolean delivered;
    private String trackingId;
    private String errorMessage;
    private Instant deliveryTimestamp;

    public SecurePostResponse() {
    }

    public boolean isDelivered() {
        return delivered;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getDeliveryTimestamp() {
        return deliveryTimestamp;
    }
}