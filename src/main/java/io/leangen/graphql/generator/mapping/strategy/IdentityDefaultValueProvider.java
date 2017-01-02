package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.metadata.QueryArgument;
import io.leangen.graphql.metadata.QueryArgumentDefaultValue;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class IdentityDefaultValueProvider implements DefaultValueProvider {

    public final QueryArgumentDefaultValue value;

    public IdentityDefaultValueProvider(QueryArgumentDefaultValue value) {
        this.value = value;
    }
    
    @Override
    public QueryArgumentDefaultValue getDefaultValue(QueryArgument argument, InputDeserializer inputDeserializer, BuildContext buildContext) {
        return value;
    }
}
