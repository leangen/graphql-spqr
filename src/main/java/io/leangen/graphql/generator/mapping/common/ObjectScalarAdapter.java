package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLScalarType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.util.Scalars;

import java.lang.reflect.AnnotatedType;
import java.util.Map;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ObjectScalarAdapter extends CachingMapper<GraphQLScalarType, GraphQLScalarType> {

    @Override
    public GraphQLScalarType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLDirective[] directives = buildContext.directiveBuilder.buildScalarTypeDirectives(javaType).stream()
                .map(directive -> operationMapper.toGraphQLDirective(directive, buildContext))
                .toArray(GraphQLDirective[]::new);
        return GenericTypeReflector.isSuperType(Map.class, javaType.getType())
                ? Scalars.graphQLMapScalar(typeName, directives)
                : Scalars.graphQLObjectScalar(typeName, directives);
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return toGraphQLType(typeName, javaType, operationMapper, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLScalar.class)
                || Object.class.equals(type.getType())
                || GenericTypeReflector.isSuperType(Map.class, type.getType());
    }
}
