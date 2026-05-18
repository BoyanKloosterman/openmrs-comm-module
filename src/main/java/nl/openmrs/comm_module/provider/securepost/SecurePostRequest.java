package nl.openmrs.comm_module.provider.securepost;

public class SecurePostRequest {

    private String format;
    private String recipient;
    private String body;
    private String subject;

    public SecurePostRequest() {
    }

    public SecurePostRequest(String format, String recipient, String body, String subject) {
        this.format = format;
        this.recipient = recipient;
        this.body = body;
        this.subject = subject;
    }

    public String getFormat() {
        return format;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getBody() {
        return body;
    }

    public String getSubject() {
        return subject;
    }
}