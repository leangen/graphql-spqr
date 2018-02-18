package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLScalarType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.strategy.ScalarMappingStrategy;
import io.leangen.graphql.util.Scalars;

import java.lang.reflect.AnnotatedType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ObjectScalarAdapter extends CachingMapper<GraphQLScalarType, GraphQLScalarType> implements OutputConverter<Object, Map<String, ?>> {

    private final ScalarMappingStrategy scalarStrategy;
    
    private static final AnnotatedType MAP = GenericTypeReflector.annotate(LinkedHashMap.class);

    public ObjectScalarAdapter(ScalarMappingStrategy scalarStrategy) {
        this.scalarStrategy = Objects.requireNonNull(scalarStrategy);
    }

    @Override
    public GraphQLScalarType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        buildContext.knownInputTypes.add(typeName);
        return Scalars.graphQLObjectScalar(typeName);
    }
    
    @Override
    public GraphQLScalarType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        buildContext.knownTypes.add(typeName);
        return toGraphQLType(typeName, javaType, operationMapper, buildContext);
    }

    @Override
    public Map<String, ?> convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return resolutionEnvironment.valueMapper.fromInput(original, type.getType(), MAP);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return scalarStrategy.supports(type);
    }
}
