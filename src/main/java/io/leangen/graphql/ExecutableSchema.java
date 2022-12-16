package io.leangen.graphql;

import graphql.schema.GraphQLSchema;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.TypeRegistry;
import org.dataloader.BatchLoaderWithContext;

import java.util.Map;

public class ExecutableSchema {

    private final GraphQLSchema schema;
    private final TypeRegistry typeRegistry;
    private final Map<String, BatchLoaderWithContext<?, ?>> batchLoaders;

    public GlobalEnvironment globalEnvironment;

    public ExecutableSchema(GraphQLSchema schema, TypeRegistry typeRegistry,
                            Map<String, BatchLoaderWithContext<?, ?>> batchLoaders, GlobalEnvironment env) {
        this.schema = schema;
        this.typeRegistry = typeRegistry;
        this.batchLoaders = batchLoaders;
        this.globalEnvironment = env;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public TypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    public Map<String, BatchLoaderWithContext<?, ?>> getBatchLoaders() {
        return batchLoaders;
    }
}
