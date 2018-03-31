package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import io.leangen.graphql.util.classpath.ClassInfo;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultImplementationDiscoveryStrategy implements ImplementationDiscoveryStrategy {

    @Override
    public List<AnnotatedType> findImplementations(AnnotatedType type, String[] scanPackages, BuildContext buildContext) {
        if (Utils.isArrayEmpty(scanPackages) && Utils.isArrayNotEmpty(buildContext.basePackages)) {
            scanPackages = buildContext.basePackages;
        }
        return ClassUtils.findImplementations(type, this::includeAutoDiscoveredType, scanPackages).stream()
                .filter(impl -> !ClassUtils.isMissingTypeParameters(impl.getType()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean includeAutoDiscoveredType(ClassInfo classInfo) {
        return classInfo.getAnnotations().stream()
                .noneMatch(ann -> ann.getClassName().equals(GraphQLIgnore.class.getName()));
    }
}
