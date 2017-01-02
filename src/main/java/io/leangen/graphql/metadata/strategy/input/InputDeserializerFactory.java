package io.leangen.graphql.metadata.strategy.input;

import java.util.List;

import io.leangen.graphql.metadata.QueryArgument;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface InputDeserializerFactory {
    
    InputDeserializer getDeserializer(List<QueryArgument> arguments);
}
