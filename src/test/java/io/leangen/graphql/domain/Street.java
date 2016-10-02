package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.NonNull;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bojan.tomic on 3/21/16.
 */
public class Street {

    private String name;
    private Integer number;

    public Street(String name, Integer number) {
        this.name = name;
        this.number = number;
    }

    @GraphQLQuery(name = "name", description = "Street name")
    public @NonNull String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @GraphQLQuery(name = "number", description = "House number")
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    @GraphQLQuery(name = "uri", description = "xxx")
    public URI getUri() throws URISyntaxException {
        return new URI("scheme://random.shit");
    }
}
