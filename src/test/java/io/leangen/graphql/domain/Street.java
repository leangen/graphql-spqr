package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLNonNull;
import org.eclipse.microprofile.graphql.Query;

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

    @Query(value = "name", description = "Street name")
    public @GraphQLNonNull String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Query(value = "number", description = "House number")
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    @Query(value = "uri", description = "A uniform resource identifier")
    public URI getUri() throws URISyntaxException {
        return new URI("scheme://random.street");
    }
}
