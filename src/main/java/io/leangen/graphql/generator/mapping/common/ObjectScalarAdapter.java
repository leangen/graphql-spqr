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
import java.util.function.Function;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.Query;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Scalars;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ObjectScalarAdapter implements TypeMapper, OutputConverter<Object, Map<String, ?>> {

    private final AnnotatedType MAP = GenericTypeReflector.annotate(LinkedHashMap.class);
    private final Map<Type, Set<Type>> abstractComponentTypes = new HashMap<>();
    
    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        if (buildContext.knownTypes.contains(javaType)) {
            return new GraphQLTypeReference(Scalars.getScalarTypeName(javaType));
        }
        buildContext.knownTypes.add(javaType);
        
        abstractTypes.addAll(collectAbstract(javaType, new HashSet<>(), buildContext.queryRepository::getChildQueries, buildContext));
        return Scalars.graphQLObjectScalar(javaType);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        if (buildContext.knownTypes.contains(javaType)) {
            return new GraphQLTypeReference(Scalars.getScalarTypeName(javaType));
        }
        buildContext.knownTypes.add(javaType);
        
        abstractTypes.addAll(collectAbstract(javaType, new HashSet<>(), buildContext.queryRepository::getInputDomainQueries, buildContext));
        return Scalars.graphQLObjectScalar(javaType);
    }

    @Override
    public Map<String, ?> convertOutput(Object original, AnnotatedType type, InputDeserializer inputDeserializer, ExecutionContext executionContext) {
        return inputDeserializer.deserialize(original, type.getType(), MAP);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLScalar.class);
    }

    private Set<Type> collectAbstract(AnnotatedType javaType, Set<Type> seen, Function<AnnotatedType, Collection<Query>> queryExtractor, BuildContext buildContext) {
        javaType = buildContext.executionContext.getMappableType(javaType);
        if (GraphQLUtils.isScalar(javaType.getType())) {
            return Collections.emptySet();
        }
        if (GenericTypeReflector.isSuperType(Collection.class, javaType.getType())) {
            return collectAbstractInner(ClassUtils.getTypeArguments(javaType)[0], seen, queryExtractor, buildContext);
        }
        if (GenericTypeReflector.isSuperType(Map.class, javaType.getType())) {
            Set<Type> abstractTypes = collectAbstractInner(ClassUtils.getTypeArguments(javaType)[0], seen, queryExtractor, buildContext);
            abstractTypes.addAll(collectAbstractInner(ClassUtils.getTypeArguments(javaType)[1], seen, queryExtractor, buildContext));
            return abstractTypes;
        }
        return collectAbstractInner(javaType, seen, queryExtractor, buildContext);
    }
    
    private Set<Type> collectAbstractInner(AnnotatedType javaType, Set<Type> seen, Function<AnnotatedType, Collection<Query>> queryExtractor, BuildContext buildContext) {
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
        queryExtractor.apply(javaType).forEach(childQuery -> abstractTypes.addAll(collectAbstract(childQuery.getJavaType(), seen, queryExtractor, buildContext)));
        abstractComponentTypes.put(javaType.getType(), abstractTypes);
        return abstractTypes;
    }
}
