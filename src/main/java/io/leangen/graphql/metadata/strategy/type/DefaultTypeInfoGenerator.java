package io.leangen.graphql.metadata.strategy.type;

import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.annotations.types.GraphQLUnion;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Scalars;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class DefaultTypeInfoGenerator implements TypeInfoGenerator {

    @Override
    public String generateTypeName(AnnotatedType type) {
        if (type instanceof AnnotatedParameterizedType) {
            String baseName = generateSimpleName(type);
            StringBuilder genericName = new StringBuilder(baseName);
            Arrays.stream(((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments())
                    .map(this::generateSimpleName)
                    .forEach(argName -> genericName.append("_").append(argName));
            return genericName.toString();
        }
        return generateSimpleName(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generateTypeDescription(AnnotatedType type) {
        Optional<String>[] descriptions = new Optional[]{
                Optional.ofNullable(type.getAnnotation(GraphQLUnion.class))
                        .map(GraphQLUnion::description),
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::description),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::description)
        };
        return getFirstNonEmptyOrDefault(descriptions, "");
    }

    @Override
    public String[] getFieldOrder(AnnotatedType type) {
        return Utils.or(
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::fieldOrder),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::fieldOrder))
                .orElse(Utils.emptyArray());
    }

    @Override
    public Set<Type> findAbstractTypes(AnnotatedType rootType, BuildContext buildContext) {
        return findAbstract(rootType, buildContext).stream()
                //ignore built-in types by default as Jackson & Gson *should* be able to deal with them on their own
                .filter(type -> !ClassUtils.getRawType(type).getPackage().getName().startsWith("java."))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private String generateSimpleName(AnnotatedType type) {
        Optional<String>[] names = new Optional[]{
                Optional.ofNullable(type.getAnnotation(GraphQLUnion.class))
                        .map(GraphQLUnion::name),
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::name),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::name)
        };
        return getFirstNonEmptyOrDefault(names, ClassUtils.getRawType(type.getType()).getSimpleName());
    }

    @SuppressWarnings("WeakerAccess")
    protected Set<Type> findAbstract(AnnotatedType javaType, BuildContext buildContext) {
        javaType = buildContext.globalEnvironment.getMappableInputType(javaType);
        if (Scalars.isScalar(javaType.getType())) {
            return Collections.emptySet();
        }
        if (javaType instanceof AnnotatedParameterizedType) {
            Set<Type> abstractTypes = Arrays.stream(((AnnotatedParameterizedType) javaType).getAnnotatedActualTypeArguments())
                    .flatMap(arg -> findAbstract(arg, buildContext).stream())
                    .collect(Collectors.toSet());
            abstractTypes.addAll(findAbstractInner(javaType, buildContext));
            return abstractTypes;
        }
        if (javaType instanceof AnnotatedArrayType) {
            return findAbstract(((AnnotatedArrayType) javaType).getAnnotatedGenericComponentType(), buildContext);
        }
        if (javaType instanceof AnnotatedWildcardType || javaType instanceof AnnotatedTypeVariable) {
            throw new TypeMappingException(javaType.getType());
        }
        return findAbstractInner(javaType, buildContext);
    }

    private Set<Type> findAbstractInner(AnnotatedType javaType, BuildContext buildContext) {
        if (buildContext.abstractTypeCache.get(javaType.getType()) != null) {
            return buildContext.abstractTypeCache.get(javaType.getType());
        }
        if (buildContext.abstractTypeCache.containsKey(javaType.getType())) {
            return Collections.emptySet();
        }
        buildContext.abstractTypeCache.put(javaType.getType(), null);
        Set<Type> abstractTypes = new HashSet<>();
        if (ClassUtils.isAbstract(javaType)) {
            abstractTypes.add(javaType.getType());
        }
        buildContext.inputFieldStrategy.getInputFields(javaType)
                .forEach(childQuery -> abstractTypes.addAll(findAbstract(childQuery.getJavaType(), buildContext)));
        buildContext.abstractTypeCache.put(javaType.getType(), abstractTypes);
        return abstractTypes;
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
    private String getFirstNonEmptyOrDefault(Optional<String>[] optionals, String defaultValue) {
        return Arrays.stream(optionals)
                .map(opt -> opt.filter(Utils::notEmpty))
                .reduce(Utils::or)
                .map(opt -> opt.orElse(defaultValue))
                .get();
    }
}
