package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class AbstractInterfaceMappingStrategy implements InterfaceMappingStrategy {

    private final boolean mapClasses;
    private final List<Predicate<Class>> filters;
    private boolean ignoreUnresolvable;

    private static final Logger log = LoggerFactory.getLogger(AbstractInterfaceMappingStrategy.class);

    protected AbstractInterfaceMappingStrategy(boolean mapClasses) {
        this.mapClasses = mapClasses;
        this.filters = new ArrayList<>();
        this.ignoreUnresolvable = false;
    }

    public AbstractInterfaceMappingStrategy withUnresolvableInterfacesIgnored() {
        this.ignoreUnresolvable = true;
        return this;
    }

    @SafeVarargs
    public final AbstractInterfaceMappingStrategy withFilters(Predicate<Class>... filters) {
        Collections.addAll(this.filters, filters);
        return this;
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
                .filter(filters.stream().reduce(Predicate::and).orElseGet(Utils::acceptAll))
                .map(inter -> getExactSuperType(type, inter))
                .filter(Objects::nonNull)
                .forEach(inter -> collectInterfaces(inter, interfaces));
        Class superClass = clazz.getSuperclass();
        if (superClass != Object.class && superClass != null) {
            collectInterfaces(GenericTypeReflector.getExactSuperType(type, superClass), interfaces);
        }
    }

    private AnnotatedType getExactSuperType(AnnotatedType type, Class inter) {
        AnnotatedType resolved = GenericTypeReflector.getExactSuperType(type, inter);
        if (resolved == null) {
            if (!ignoreUnresolvable) {
                throw TypeMappingException.unresolvableSuperType(inter, type.getType());
            }
            log.warn("Interface {} can not be resolved as a super type of {} so it will be ignored",
                    inter.getName(), type.getType().getTypeName());
        }
        return resolved;
    }
}
