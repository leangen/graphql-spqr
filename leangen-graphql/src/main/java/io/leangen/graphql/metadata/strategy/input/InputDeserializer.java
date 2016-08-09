package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.Type;

/**
 * Created by bojan.tomic on 5/25/16.
 */
public interface InputDeserializer {

	<T> T deserialize(Object graphQlInput, Type type);
}
