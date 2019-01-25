package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.types.Interface;
import io.leangen.graphql.annotations.types.Type;

@Type("Pet")
@Interface(implementationAutoDiscovery = true)
public interface Pet {
    String getSound();
    Human getOwner();
}
