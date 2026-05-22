package nl.openmrs.comm_module.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** Alleen actief als openmrs.datasource.url een niet-lege JDBC-URL is. */
public class OnOpenmrsDataSourceConfigured implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String url = context.getEnvironment().getProperty("openmrs.datasource.url");
        return StringUtils.hasText(url);
    }
}
