package nl.openmrs.comm_module.provider.securepost;

public class SecurePostAuthRequest {

    private String clientId;
    private String clientSecret;

    public SecurePostAuthRequest() {
    }

    public SecurePostAuthRequest(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}