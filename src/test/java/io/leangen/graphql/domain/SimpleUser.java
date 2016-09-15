package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.RelayId;

/**
 * Created by bojan.tomic on 6/24/16.
 */
public class SimpleUser {

    private String fullName;
    private SimpleUser friend;

    public SimpleUser() {
    }

    public SimpleUser(String fullName) {
        this.fullName = fullName;
    }

    public SimpleUser(String fullName, SimpleUser friend) {
        this.fullName = fullName;
        this.friend = friend;
    }

    @GraphQLQuery(name = "fullName", description = "A person's name")
    public @RelayId String getFullName() {
        return fullName;
    }

    @GraphQLQuery(name = "friend", description = "A person's name")
    public SimpleUser getFriend() {
        return friend;
    }

    @GraphQLQuery(name = "education")
    public Education getEducation(int year) {
        return new Education("alma mater", year - 1, year + 1);
    }
}
