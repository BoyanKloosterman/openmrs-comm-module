package nl.openmrs.comm_module.provider.swiftsend;

public class SwiftSendApiException extends RuntimeException {

    public SwiftSendApiException(String message) {
        super(message);
    }

    public SwiftSendApiException(String message, Throwable cause) {
        super(message, cause);
    }
}