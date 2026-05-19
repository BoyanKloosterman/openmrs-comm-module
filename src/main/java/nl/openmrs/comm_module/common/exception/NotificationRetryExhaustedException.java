package nl.openmrs.comm_module.common.exception;

public class NotificationRetryExhaustedException extends RuntimeException {

    public NotificationRetryExhaustedException(String message) {
        super(message);
    }
}