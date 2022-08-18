package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bojan.tomic on 3/21/16.
 * Altered getters to look like record.
 */
public class Street {

    private String name;
    private Integer number;

    public Street(String name, Integer number) {
        this.name = name;
        this.number = number;
    }

    @GraphQLQuery(name = "name", description = "Street name")
    public @GraphQLNonNull String name() {
        return name;
    }

    @GraphQLQuery(name = "number", description = "House number")
    public Integer number() {
        return number;
    }

    @GraphQLQuery(name = "uri", description = "A uniform resource identifier")
    public URI getUri() throws URISyntaxException {
        return new URI("scheme://random.street");
    }
}
