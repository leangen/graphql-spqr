package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLQuery;

public class Robot implements Character {

    private final String model;
    private final String name;

    public Robot(String model, String name) {
        this.model = model;
        this.name = name;
    }

    @GraphQLQuery(name = "id")
    public @GraphQLId(relayId = true) String getModel() {
        return model;
    }

    @Override
    public String getName() {
        return name;
    }
}
