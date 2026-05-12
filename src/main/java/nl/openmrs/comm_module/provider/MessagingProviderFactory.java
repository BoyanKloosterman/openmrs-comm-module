package nl.openmrs.comm_module.provider;

import nl.openmrs.comm_module.common.exception.ProviderNotFoundException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class MessagingProviderFactory {

    private final Map<MessagingProviderType, MessagingProvider> providers =
            new EnumMap<>(MessagingProviderType.class);

    public MessagingProviderFactory(List<MessagingProvider> providerList) {
        for (MessagingProvider provider : providerList) {
            providers.put(provider.getType(), provider);
        }
    }

    public MessagingProvider getProvider(MessagingProviderType providerType) {
        MessagingProvider provider = providers.get(providerType);

        if (provider == null) {
            throw new ProviderNotFoundException(
                    "No messaging provider configured for type: " + providerType
            );
        }

        return provider;
    }
}