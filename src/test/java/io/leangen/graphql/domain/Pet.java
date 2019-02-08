package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.types.Interface;
import org.eclipse.microprofile.graphql.Type;

@Type("Pet")
@Interface(implementationAutoDiscovery = true)
public interface Pet {
    String getSound();
    Human getOwner();
}
