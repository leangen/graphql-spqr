package io.leangen.graphql.generator;

import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Scalars;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeCache {

    private final Map<String, GraphQLType> knownTypes;
    private final Map<Type, Set<Type>> abstractComponents;


    TypeCache(Set<GraphQLType> knownTypes) {
        this.knownTypes = knownTypes.stream().collect(Collectors.toMap(GraphQLType::getName, Function.identity()));
        this.abstractComponents = new HashMap<>();
    }

    public void register(String typeName) {
        knownTypes.put(typeName, null);
    }

    public boolean contains(String typeName) {
        return knownTypes.containsKey(typeName);
    }

    public GraphQLType resolveType(String typeName) {
        GraphQLType resolved = knownTypes.get(typeName);
        if (resolved instanceof GraphQLTypeReference) {
            throw new IllegalStateException("Type " + typeName + " is not yet resolvable");
        }
        return resolved;
    }

    void completeType(GraphQLOutputType type) {
        type = (GraphQLOutputType) GraphQLUtils.unwrap(type);
        if (!(type instanceof GraphQLTypeReference)) {
            knownTypes.put(type.getName(), type);
        }
    }

    void resolveTypeReferences(TypeRepository typeRepository) {
        typeRepository.resolveTypeReferences(knownTypes);
    }

    public Set<Type> findAbstract(AnnotatedType javaType, BuildContext buildContext) {
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
        buildContext.inputFieldStrategy.getInputFields(javaType)
                .forEach(childQuery -> abstractTypes.addAll(findAbstract(childQuery.getJavaType(), buildContext)));
        abstractComponents.put(javaType.getType(), abstractTypes);
        return abstractTypes;
    }
}
