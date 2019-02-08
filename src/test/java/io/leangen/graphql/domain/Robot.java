package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLId;
import org.eclipse.microprofile.graphql.Query;

public class Robot implements Character {

    private String model;
    private String name;

    public Robot(String model, String name) {
        this.model = model;
        this.name = name;
    }

    @Query(value = "id")
    public @GraphQLId(relayId = true) String getModel() {
        return model;
    }

    @Override
    public String getName() {
        return name;
    }
}
