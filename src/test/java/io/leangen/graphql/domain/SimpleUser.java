package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.Query;

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

    @Query(value = "fullName", description = "A person's name")
    public String getFullName() {
        return fullName;
    }

    @Query(value = "friend", description = "A person's name")
    public SimpleUser getFriend() {
        return friend;
    }

    @Query(value = "education")
    public Education getEducation(int year) {
        return new Education("alma mater", year - 1, year + 1);
    }
}
