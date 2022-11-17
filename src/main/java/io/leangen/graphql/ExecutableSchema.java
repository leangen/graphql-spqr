package io.leangen.graphql;

import graphql.schema.GraphQLSchema;
import org.dataloader.BatchLoaderWithContext;

import java.util.Map;

public class ExecutableSchema {

    private final GraphQLSchema schema;
    private final Map<String, BatchLoaderWithContext<?, ?>> batchLoaders;

    public ExecutableSchema(GraphQLSchema schema, Map<String, BatchLoaderWithContext<?, ?>> batchLoaders) {
        this.schema = schema;
        this.batchLoaders = batchLoaders;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public Map<String, BatchLoaderWithContext<?, ?>> getBatchLoaders() {
        return batchLoaders;
    }
}
