package io.leangen.graphql.query;

import java.lang.reflect.Type;

/**
 * Created by bojan.tomic on 4/13/16.
 */
public interface IdTypeMapper {

	boolean supports(Type type);
	<T> String serialize(T id);
	<T> T deserialize(String id, Type type);
}
