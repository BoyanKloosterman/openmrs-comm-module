package nl.openmrs.comm_module.provider.swiftsend;

import java.util.List;

public class SwiftSendRequest {

    private String type;
    private List<String> recipients;
    private String content;

    public SwiftSendRequest() {
    }

    public SwiftSendRequest(String type, List<String> recipients, String content) {
        this.type = type;
        this.recipients = recipients;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public String getContent() {
        return content;
    }
}