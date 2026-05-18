package nl.openmrs.comm_module.provider.securepost;

import java.time.Instant;

public class SecurePostTokenResponse {

    private String accessToken;
    private String tokenType;
    private int expiresIn;
    private Instant issuedAt;

    public SecurePostTokenResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}