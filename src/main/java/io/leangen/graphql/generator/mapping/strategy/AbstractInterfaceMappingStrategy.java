package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class AbstractInterfaceMappingStrategy implements InterfaceMappingStrategy {

    private final boolean mapClasses;

    protected AbstractInterfaceMappingStrategy(boolean mapClasses) {
        this.mapClasses = mapClasses;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return (mapClasses || ClassUtils.getRawType(type.getType()).isInterface()) && supportsInterface(type);
    }

    protected abstract boolean supportsInterface(AnnotatedType inter);

    @Override
    public Collection<AnnotatedType> getInterfaces(AnnotatedType type) {
        Map<Class<?>, AnnotatedType> interfaces = new HashMap<>();
        collectInterfaces(type, interfaces);
        return interfaces.values();
    }

    private void collectInterfaces(AnnotatedType type, Map<Class<?>, AnnotatedType> interfaces) {
        Class clazz = ClassUtils.getRawType(type.getType());
        if (interfaces.containsKey(clazz)) {
            return;
        }
        if (clazz.isInterface() || mapClasses) {
            if (supports(type)) {
                interfaces.put(clazz, type);
            }
        }
        Arrays.stream(clazz.getInterfaces())
                .map(inter -> GenericTypeReflector.getExactSuperType(type, inter))
                .forEach(inter -> collectInterfaces(inter, interfaces));
        Class superClass = clazz.getSuperclass();
        if (superClass != Object.class && superClass != null) {
            collectInterfaces(GenericTypeReflector.getExactSuperType(type, superClass), interfaces);
        }
    }
}
