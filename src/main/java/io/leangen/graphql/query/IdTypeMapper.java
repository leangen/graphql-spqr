package io.leangen.graphql.query;

import java.lang.reflect.Type;

public interface IdTypeMapper {

    boolean supports(Type type);

    <T> String serialize(T id);

    <T> T deserialize(String id, Type type);
}
