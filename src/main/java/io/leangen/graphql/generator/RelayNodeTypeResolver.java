package io.leangen.graphql.generator;

import javax.naming.OperationNotSupportedException;

import graphql.GraphQLException;
import graphql.TypeResolutionEnvironment;
import graphql.relay.Relay;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RelayNodeTypeResolver implements TypeResolver {

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        String graphQLTypeName = new Relay().fromGlobalId((String) env.getArguments().get("id")).getType();
        return (GraphQLObjectType) env.getSchema().getType(graphQLTypeName);
    }

    @Override
    public GraphQLObjectType getType(Object object) {
        throw new GraphQLException(new OperationNotSupportedException("Simple type resolution not supported"));
    }
}
