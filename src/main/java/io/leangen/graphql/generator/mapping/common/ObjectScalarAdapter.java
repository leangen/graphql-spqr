package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import graphql.schema.GraphQLScalarType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Scalars;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ObjectScalarAdapter extends CachingAbstractAwareMapper<GraphQLScalarType, GraphQLScalarType> implements OutputConverter<Object, Map<String, ?>> {

    private final AnnotatedType MAP = GenericTypeReflector.annotate(LinkedHashMap.class);
    private final Map<Type, Set<Type>> abstractComponentTypes = new HashMap<>();

    @Override
    public GraphQLScalarType toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        return Scalars.graphQLObjectScalar(typeName, javaType);
    }
    
    @Override
    public GraphQLScalarType toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        return toGraphQLType(typeName, javaType, abstractTypes, queryGenerator, buildContext);
    }

    @Override
    protected void registerAbstract(AnnotatedType type, Set<Type> abstractTypes, BuildContext buildContext) {
        abstractTypes.addAll(collectAbstract(type, new HashSet<>(), buildContext));
    }

    @Override
    public Map<String, ?> convertOutput(Object original, AnnotatedType type, InputDeserializer inputDeserializer, ExecutionContext executionContext) {
        return inputDeserializer.deserialize(original, type.getType(), MAP);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLScalar.class);
    }

    private Set<Type> collectAbstract(AnnotatedType javaType, Set<Type> seen, BuildContext buildContext) {
        javaType = buildContext.executionContext.getMappableType(javaType);
        if (GraphQLUtils.isScalar(javaType.getType())) {
            return Collections.emptySet();
        }
        if (GenericTypeReflector.isSuperType(Collection.class, javaType.getType())) {
            return collectAbstractInner(ClassUtils.getTypeArguments(javaType)[0], seen, buildContext);
        }
        if (GenericTypeReflector.isSuperType(Map.class, javaType.getType())) {
            Set<Type> abstractTypes = collectAbstractInner(ClassUtils.getTypeArguments(javaType)[0], seen, buildContext);
            abstractTypes.addAll(collectAbstractInner(ClassUtils.getTypeArguments(javaType)[1], seen, buildContext));
            return abstractTypes;
        }
        return collectAbstractInner(javaType, seen, buildContext);
    }

    private Set<Type> collectAbstractInner(AnnotatedType javaType, Set<Type> seen, BuildContext buildContext) {
        if (abstractComponentTypes.containsKey(javaType.getType())) {
            return abstractComponentTypes.get(javaType.getType());
        }
        if (seen.contains(javaType.getType())) {
            return Collections.emptySet();
        }
        seen.add(javaType.getType());
        Set<Type> abstractTypes = new HashSet<>();
        if (ClassUtils.isAbstract(javaType)) {
            abstractTypes.add(javaType.getType());
        }
        buildContext.queryRepository.getInputDomainQueries(javaType)
                .forEach(childQuery -> abstractTypes.addAll(collectAbstract(childQuery.getJavaType(), seen, buildContext)));
        abstractComponentTypes.put(javaType.getType(), abstractTypes);
        return abstractTypes;
    }
}
