package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.types.GraphQLInterface;

@GraphQLInterface(name = "Pet", implementationAutoDiscovery = true)
public interface Pet {
    String getSound();
    Human getOwner();
}
