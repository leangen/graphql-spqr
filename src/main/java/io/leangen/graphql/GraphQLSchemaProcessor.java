package io.leangen.graphql;

import graphql.schema.GraphQLSchema;

/**
 * Created by bojan.tomic on 3/2/16.
 */
public interface GraphQLSchemaProcessor {

	GraphQLSchema.Builder process(GraphQLSchema.Builder schemaBuilder);
}
