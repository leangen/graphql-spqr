package io.leangen.graphql.domain;

import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Type;

/**
 * Created by bojan.tomic on 4/11/16.
 */
@Type(value = "Person")
public interface Person {

    @Query(value = "name", description = "A person's name")
    String getName();
}
