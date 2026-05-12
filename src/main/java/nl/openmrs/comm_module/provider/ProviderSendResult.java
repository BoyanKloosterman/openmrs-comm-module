package nl.openmrs.comm_module.provider;

public class ProviderSendResult {

    private final boolean successful;
    private final String providerMessageId;
    private final String status;
    private final String errorMessage;

    private ProviderSendResult(boolean successful, String providerMessageId, String status, String errorMessage) {
        this.successful = successful;
        this.providerMessageId = providerMessageId;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public static ProviderSendResult success(String providerMessageId) {
        return new ProviderSendResult(true, providerMessageId, "SENT", null);
    }

    public static ProviderSendResult submitted(String providerMessageId) {
        return new ProviderSendResult(true, providerMessageId, "SUBMITTED", null);
    }

    public static ProviderSendResult failed(String errorMessage) {
        return new ProviderSendResult(false, null, "FAILED", errorMessage);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}