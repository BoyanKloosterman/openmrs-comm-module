package nl.openmrs.comm_module.provider;

public enum MessagingProviderType {
    SWIFTSEND("provider.swiftsend"),
    SECUREPOST("provider.securepost"),
    LEGACYLINK("provider.legacylink"),
    ASYNCFLOW("provider.asyncflow");

    private final String routingKey;

    MessagingProviderType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}