package org.zalando.putittorest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.zalando.putittorest.annotation.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

final class Registry {

    private static final Logger LOG = LoggerFactory.getLogger(Registry.class);

    private final BeanDefinitionRegistry registry;

    Registry(final BeanDefinitionRegistry registry) {
        this.registry = registry;
    }

    public boolean isRegistered(final String name) {
        return registry.isBeanNameInUse(name);
    }

    public <T> String register(final Class<T> type, final Supplier<BeanDefinitionBuilder> factory) {
        final String name = UPPER_CAMEL.to(LOWER_CAMEL, type.getSimpleName());

        if (isRegistered(name)) {
            LOG.debug("Bean [{}] is already registered, skipping it.");
            return name;
        }

        registry.registerBeanDefinition(name, factory.get().getBeanDefinition());

        return name;
    }

    public <T> String register(final String id, final Class<T> type,
            final Supplier<BeanDefinitionBuilder> factory) {

        final String name = generateBeanName(id, type);

        if (isRegistered(name)) {
            LOG.debug("Bean [{}] is already registered, skipping it.");
            return name;
        }

        final AbstractBeanDefinition definition = factory.get().getBeanDefinition();

        definition.addQualifier(new AutowireCandidateQualifier(RestClient.class, id));
        definition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, id));

        registry.registerBeanDefinition(name, definition);

        return name;
    }

    public static <T> String generateBeanName(final String id, final Class<T> type) {
        return LOWER_HYPHEN.to(LOWER_CAMEL, id) + type.getSimpleName();
    }

    public static BeanReference ref(final String beanName) {
        return new RuntimeBeanReference(beanName);
    }

    public static List<Object> list(final Object... elements) {
        final ManagedList<Object> list = new ManagedList<>();
        Collections.addAll(list, elements);
        return list;
    }

}
