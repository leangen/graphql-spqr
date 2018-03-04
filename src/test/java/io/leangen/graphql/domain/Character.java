package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.types.GraphQLUnion;

@GraphQLUnion(name = "Character", possibleTypeAutoDiscovery = true)
public interface Character {

    String getName();
}
