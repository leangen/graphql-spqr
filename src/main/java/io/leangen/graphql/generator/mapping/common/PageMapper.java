package io.leangen.graphql.generator.mapping.common;

import graphql.relay.Edge;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.relay.Connection;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class PageMapper extends ObjectTypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        AnnotatedType edgeType = GenericTypeReflector.getTypeParameter(javaType, Connection.class.getTypeParameters()[0]);
        AnnotatedType nodeType = GenericTypeReflector.getTypeParameter(edgeType, Edge.class.getTypeParameters()[0]);
        String connectionName = buildContext.typeInfoGenerator.generateTypeName(nodeType, buildContext.messageBundle) + "Connection";
        if (buildContext.typeCache.contains(connectionName)) {
            return new GraphQLTypeReference(connectionName);
        }
        buildContext.typeCache.register(connectionName);
        GraphQLOutputType type = env.operationMapper.toGraphQLType(nodeType, env);
        GraphQLNamedType unwrapped = GraphQLUtils.unwrap(type);
        String baseName = type instanceof GraphQLList ? unwrapped.getName() + "List" : unwrapped.getName();
        List<GraphQLFieldDefinition> edgeFields = getFields(baseName + "Edge", edgeType, env).stream()
                .filter(field -> !GraphQLUtils.isRelayEdgeField(field))
                .collect(Collectors.toList());
        GraphQLObjectType edge = buildContext.relay.edgeType(baseName, type, null, edgeFields);
        List<GraphQLFieldDefinition> connectionFields = getFields(baseName + "Connection", javaType, env).stream()
                .filter(field -> !GraphQLUtils.isRelayConnectionField(field))
                .collect(Collectors.toList());
        buildContext.typeRegistry.getDiscoveredTypes().add(Relay.pageInfoType);
        return buildContext.relay.connectionType(baseName, edge, connectionFields);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        throw new UnsupportedOperationException("Replay page type can not be used as input type");
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return ClassUtils.isSuperClass(Connection.class, type);
    }
}
