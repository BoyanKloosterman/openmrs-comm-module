package nl.openmrs.comm_module.common.exception;

public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String message) {
        super(message);
    }
}