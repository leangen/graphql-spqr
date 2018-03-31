package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Scalars;
import io.leangen.graphql.util.classpath.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AutoDiscoveryAbstractInputHandler implements AbstractInputHandler {

    private final Map<Type, Set<Type>> abstractComponents = new HashMap<>();

    @SuppressWarnings("WeakerAccess")
    protected final static Predicate<ClassInfo> NON_ABSTRACT = info ->
            (info.getModifier() & (Modifier.ABSTRACT | Modifier.INTERFACE)) == 0;

    @SuppressWarnings("WeakerAccess")
    protected final static Predicate<ClassInfo> NON_IGNORED = info ->
            info.getAnnotations().stream().noneMatch(ann -> ann.getClassName().equals(GraphQLIgnore.class.getName()));

    private static final Logger log = LoggerFactory.getLogger(AutoDiscoveryAbstractInputHandler.class);

    @Override
    public Set<Type> findConstituentAbstractTypes(AnnotatedType javaType, BuildContext buildContext) {
        if (Scalars.isScalar(javaType.getType())
                || ClassUtils.isSubPackage(ClassUtils.getRawType(javaType.getType()).getPackage(), "java.")
                || buildContext.scalarStrategy.isDirectlyDeserializable(javaType)) {
            return Collections.emptySet();
        }
        if (javaType instanceof AnnotatedParameterizedType) {
            Set<Type> abstractTypes = Arrays.stream(((AnnotatedParameterizedType) javaType).getAnnotatedActualTypeArguments())
                    .flatMap(arg -> findConstituentAbstractTypes(arg, buildContext).stream())
                    .collect(Collectors.toSet());
            abstractTypes.addAll(findAbstract(javaType, buildContext));
            return abstractTypes;
        }
        if (javaType instanceof AnnotatedArrayType) {
            return findConstituentAbstractTypes(((AnnotatedArrayType) javaType).getAnnotatedGenericComponentType(), buildContext);
        }
        if (javaType instanceof AnnotatedWildcardType || javaType instanceof AnnotatedTypeVariable) {
            throw new TypeMappingException(javaType.getType());
        }
        return findAbstract(javaType, buildContext);
    }

    @Override
    public List<Class> findConcreteSubTypes(Class abstractType, BuildContext buildContext) {
        List<Class> subTypes = ClassUtils.findImplementations(abstractType, NON_ABSTRACT.and(NON_IGNORED), buildContext.basePackages);
        if (subTypes.isEmpty()) {
            log.warn("No concrete subtypes of " + abstractType.getName() + " found");
        }
        return subTypes;
    }

    private Set<Type> findAbstract(AnnotatedType javaType, BuildContext buildContext) {
        if (abstractComponents.get(javaType.getType()) != null) {
            return abstractComponents.get(javaType.getType());
        }
        if (abstractComponents.containsKey(javaType.getType())) {
            return Collections.emptySet();
        }
        abstractComponents.put(javaType.getType(), null);
        Set<Type> abstractTypes = new HashSet<>();
        if (ClassUtils.isAbstract(javaType)) {
            abstractTypes.add(javaType.getType());
        }
        buildContext.inputFieldStrategy.getInputFields(javaType, buildContext.inclusionStrategy)
                .forEach(childQuery -> abstractTypes.addAll(findConstituentAbstractTypes(childQuery.getDeserializableType(), buildContext)));
        abstractComponents.put(javaType.getType(), abstractTypes);
        return abstractTypes;
    }
}
