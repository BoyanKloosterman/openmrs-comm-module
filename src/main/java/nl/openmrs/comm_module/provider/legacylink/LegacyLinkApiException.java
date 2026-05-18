package nl.openmrs.comm_module.provider.legacylink;

public class LegacyLinkApiException extends RuntimeException {

    public LegacyLinkApiException(String message) {
        super(message);
    }

    public LegacyLinkApiException(String message, Throwable cause) {
        super(message, cause);
    }
}