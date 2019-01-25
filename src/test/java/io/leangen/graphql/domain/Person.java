package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.Query;
import io.leangen.graphql.annotations.types.Type;

/**
 * Created by bojan.tomic on 4/11/16.
 */
@Type(value = "Person")
public interface Person {

    @Query(value = "name", description = "A person's name")
    String getName();
}
