package nl.openmrs.comm_module.provider.asyncflow;

public class AsyncFlowApiException extends RuntimeException {

    public AsyncFlowApiException(String message) {
        super(message);
    }

    public AsyncFlowApiException(String message, Throwable cause) {
        super(message, cause);
    }
}