package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class SuperTypeBasedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

    private final Collection<Class<?>> mappedTypes;

    public SuperTypeBasedInterfaceStrategy(Collection<Class<?>> mappedTypes) {
        this.mappedTypes = mappedTypes;
    }

    @Override
    public boolean supportsInterface(AnnotatedType inter) {
        Class<?> raw = ClassUtils.getRawType(inter.getType());
        return mappedTypes.stream().anyMatch(type -> type.isAssignableFrom(raw));
    }
}
