package io.leangen.graphql.generator.mapping.common;

import graphql.relay.Edge;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.relay.Connection;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.util.GraphQLUtils;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class PageMapper extends ObjectTypeMapper {

    public PageMapper() {
        super(false);
    }

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        AnnotatedType edgeType = GenericTypeReflector.getTypeParameter(javaType, Connection.class.getTypeParameters()[0]);
        AnnotatedType nodeType = GenericTypeReflector.getTypeParameter(edgeType, Edge.class.getTypeParameters()[0]);
        String connectionName = buildContext.typeInfoGenerator.generateTypeName(nodeType) + "Connection";
        if (buildContext.isKnownType(connectionName)) {
            return new GraphQLTypeReference(connectionName);
        }
        buildContext.registerTypeName(connectionName);
        GraphQLOutputType type = operationMapper.toGraphQLType(nodeType, buildContext);
        List<GraphQLFieldDefinition> edgeFields = getFields(edgeType, buildContext, operationMapper).stream()
                .filter(field -> !GraphQLUtils.isRelayEdgeField(field))
                .collect(Collectors.toList());
        GraphQLObjectType edge = buildContext.relay.edgeType(type.getName(), type, null, edgeFields);
        List<GraphQLFieldDefinition> connectionFields = getFields(javaType, buildContext, operationMapper).stream()
                .filter(field -> !GraphQLUtils.isRelayConnectionField(field))
                .collect(Collectors.toList());
        return buildContext.relay.connectionType(type.getName(), edge, connectionFields);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        throw new UnsupportedOperationException("Replay page type can not be used as input type");
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Connection.class, type.getType());
    }
}
