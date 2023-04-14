package io.leangen.graphql.execution.complexity;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Utils;

import java.util.Map;

public abstract class AbstractComplexityFunction implements ComplexityFunction {

    @Override
    public int getComplexity(ResolvedField field, int childScore) {
        Resolver resolver = field.getResolver();
        if (resolver == null || Utils.isEmpty(resolver.getComplexityExpression())) {
            GraphQLType fieldType = field.getFieldType();
            if (fieldType instanceof GraphQLScalarType || fieldType instanceof GraphQLEnumType) {
                return 1;
            }
            if (GraphQLUtils.isRelayConnectionType(fieldType)) {
                Integer pageSize = GraphQLUtils.getPageSize(field.getArguments());
                if (pageSize != null) {
                    return pageSize * childScore;
                }
            }
            return 1 + childScore;
        }
        try {
            return eval(resolver.getComplexityExpression(), Utils.put(field.getArguments(), "childScore", childScore));
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Invalid complexity expression \"%s\" on field %s",
                    resolver.getComplexityExpression(), field.getCoordinates()), e);
        }
    }

    protected abstract int eval(String expression, Map<String, Object> arguments) throws Exception;
}
