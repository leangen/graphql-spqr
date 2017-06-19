package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class PageMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        AnnotatedType elementType = GenericTypeReflector.getTypeParameter(javaType, Page.class.getTypeParameters()[0]);
        String connectionName = buildContext.typeInfoGenerator.generateTypeName(elementType) + "Connection";
        if (buildContext.knownTypes.contains(connectionName)) {
            return new GraphQLTypeReference(connectionName);
        }
        buildContext.knownTypes.add(connectionName);
        GraphQLOutputType type = operationMapper.toGraphQLType(elementType, abstractTypes, buildContext);
        GraphQLObjectType edge = buildContext.relay.edgeType(type.getName(), type, null, Collections.emptyList());
        return buildContext.relay.connectionType(type.getName(), edge, Collections.emptyList());
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        throw new UnsupportedOperationException("Replay page type can not be used as input type");
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Page.class, type.getType());
    }
}
