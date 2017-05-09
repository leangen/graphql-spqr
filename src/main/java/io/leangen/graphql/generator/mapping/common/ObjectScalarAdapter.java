package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import graphql.schema.GraphQLScalarType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.util.Scalars;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ObjectScalarAdapter extends CachingMapper<GraphQLScalarType, GraphQLScalarType> implements OutputConverter<Object, Map<String, ?>> {

    private final AnnotatedType MAP = GenericTypeReflector.annotate(LinkedHashMap.class);

    @Override
    public GraphQLScalarType toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        buildContext.knownInputTypes.add(typeName);
        return Scalars.graphQLObjectScalar(typeName);
    }
    
    @Override
    public GraphQLScalarType toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        buildContext.knownTypes.add(typeName);
        return toGraphQLType(typeName, javaType, abstractTypes, operationMapper, buildContext);
    }

    @Override
    protected void registerAbstract(AnnotatedType type, Set<Type> abstractTypes, BuildContext buildContext) {
        abstractTypes.addAll(collectAbstract(type, new HashSet<>(), buildContext));
    }

    @Override
    public Map<String, ?> convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return resolutionEnvironment.valueMapper.fromInput(original, type.getType(), MAP);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLScalar.class);
    }
}
