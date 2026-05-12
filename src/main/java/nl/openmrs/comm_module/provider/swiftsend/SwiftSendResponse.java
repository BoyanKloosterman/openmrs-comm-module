package nl.openmrs.comm_module.provider.swiftsend;

import java.util.List;

public class SwiftSendResponse {

    private boolean success;
    private String messageId;
    private List<String> failedRecipients;
    private String error;

    public SwiftSendResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessageId() {
        return messageId;
    }

    public List<String> getFailedRecipients() {
        return failedRecipients;
    }

    public String getError() {
        return error;
    }
}