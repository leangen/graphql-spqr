package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class AbstractInterfaceMappingStrategy implements InterfaceMappingStrategy {

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.getRawType(type.getType()).isInterface() && supportsInterface(type);
    }

    protected abstract boolean supportsInterface(AnnotatedType inter);

    @Override
    public Collection<AnnotatedType> getInterfaces(AnnotatedType type) {
        Class clazz = ClassUtils.getRawType(type.getType());
        Set<AnnotatedType> interfaces = new HashSet<>();
        do {
            Arrays.stream(clazz.getInterfaces())
                    .map(inter -> GenericTypeReflector.getExactSuperType(type, inter))
                    .filter(this::supports)
                    .forEach(interfaces::add);
        } while ((clazz = clazz.getSuperclass()) != Object.class && clazz != null);
        return interfaces;
    }
}
