package nl.openmrs.comm_module.provider.securepost;

public class SecurePostApiException extends RuntimeException {

    public SecurePostApiException(String message) {
        super(message);
    }

    public SecurePostApiException(String message, Throwable cause) {
        super(message, cause);
    }
}