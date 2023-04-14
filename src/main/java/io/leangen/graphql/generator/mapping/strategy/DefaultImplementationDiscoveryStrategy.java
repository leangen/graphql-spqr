package io.leangen.graphql.generator.mapping.strategy;

import io.github.classgraph.ClassInfo;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.leangen.graphql.util.ClassFinder.ALL;
import static io.leangen.graphql.util.ClassFinder.NON_IGNORED;
import static io.leangen.graphql.util.ClassFinder.PUBLIC;

public class DefaultImplementationDiscoveryStrategy implements ImplementationDiscoveryStrategy {

    private final List<Predicate<ClassInfo>> filters;
    private final List<Class<?>> additionalImplementations;

    public DefaultImplementationDiscoveryStrategy() {
        this.filters = new ArrayList<>();
        this.filters.add(PUBLIC);
        this.additionalImplementations = new ArrayList<>();
    }

    @Override
    public List<AnnotatedType> findImplementations(AnnotatedType type, boolean autoDiscover, String[] scanPackages, BuildContext buildContext) {
        if (Utils.isEmpty(scanPackages) && Utils.isNotEmpty(buildContext.basePackages)) {
            scanPackages = buildContext.basePackages;
        }
        Predicate<ClassInfo> filter = NON_IGNORED.and(filters.stream().reduce(Predicate::and).orElse(ALL));

        List<AnnotatedType> additionalImpls = additionalImplementationsOf(type);
        if (!autoDiscover) {
            return additionalImpls;
        }
        List<AnnotatedType> discoveredImpls = buildContext.classFinder.findImplementations(type, filter, false, scanPackages);
        Set<Class<?>> seen = new HashSet<>(discoveredImpls.size() + additionalImpls.size());
        return Stream.concat(additionalImpls.stream(), discoveredImpls.stream())
                .filter(impl -> seen.add(GenericTypeReflector.erase(impl.getType())))
                .collect(Collectors.toList());
    }

    public DefaultImplementationDiscoveryStrategy withNonPublicClasses() {
        this.filters.remove(PUBLIC);
        return this;
    }

    @SafeVarargs
    public final DefaultImplementationDiscoveryStrategy withFilters(Predicate<ClassInfo>... filters) {
        Collections.addAll(this.filters, filters);
        return this;
    }

    public DefaultImplementationDiscoveryStrategy withAdditionalImplementations(Class<?>... additionalImplementations) {
        Collections.addAll(this.additionalImplementations, additionalImplementations);
        return this;
    }

    private List<AnnotatedType> additionalImplementationsOf(AnnotatedType type) {
        return additionalImplementations.stream()
                .filter(impl -> ClassUtils.isSuperClass(type, impl))
                .map(impl -> {
                    AnnotatedType implType = GenericTypeReflector.getExactSubType(type, impl);
                    if (implType == null || ClassUtils.isMissingTypeParameters(implType.getType())) {
                        throw new TypeMappingException(String.format("%s could not be resolved as a subtype of %s", impl.getName(), type.getType().getTypeName()));
                    }
                    return implType;
                })
                .collect(Collectors.toList());
    }
}
