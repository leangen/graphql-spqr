package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

/**
 * Created by bojan.tomic on 4/11/16.
 */
@GraphQLType(name = "Person")
public interface Person {

    @GraphQLQuery(name = "name", description = "A person's name")
    String getName();
}
