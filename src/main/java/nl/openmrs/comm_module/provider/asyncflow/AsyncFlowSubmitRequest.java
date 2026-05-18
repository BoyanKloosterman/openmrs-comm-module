package nl.openmrs.comm_module.provider.asyncflow;

public class AsyncFlowSubmitRequest {

    private String destination;
    private String content;
    private String priority;

    public AsyncFlowSubmitRequest() {
    }

    public AsyncFlowSubmitRequest(String destination, String content, String priority) {
        this.destination = destination;
        this.content = content;
        this.priority = priority;
    }

    public String getDestination() {
        return destination;
    }

    public String getContent() {
        return content;
    }

    public String getPriority() {
        return priority;
    }
}