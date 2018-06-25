package io.leangen.graphql.extension.common.jackson;

import com.fasterxml.jackson.databind.node.POJONode;
import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;

import java.lang.reflect.AnnotatedType;

public class JacksonObjectScalarMapper implements TypeMapper {

    @Override
    public GraphQLScalarType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return JacksonObjectScalars.toGraphQLScalarType(javaType.getType());
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        if (POJONode.class.equals(javaType.getType())) {
            throw new UnsupportedOperationException(POJONode.class.getSimpleName() + " can not be used as input");
        }
        return toGraphQLType(javaType, operationMapper, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return JacksonObjectScalars.isScalar(type.getType());
    }
}
