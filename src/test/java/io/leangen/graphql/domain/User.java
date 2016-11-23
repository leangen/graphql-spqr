package io.leangen.graphql.domain;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.RelayId;

/**
 * Created by bojan.tomic on 3/5/16.
 */
public class User<T> implements Person {

    public String name;

    @GraphQLQuery(name = "title", description = "A person's title")
    public String title;

    @RelayId
    @GraphQLQuery(name = "id", description = "A person's id")
    public Integer id;

    @GraphQLQuery(name = "uuid", description = "A person's uuid")
    public UUID uuid;

    @GraphQLQuery(name = "regDate", description = "Date of registration")
    public Date registrationDate;

    @GraphQLQuery(name = "addresses", description = "A person's address")
    public Collection<Address> addresses;

    @GraphQLQuery(name = "gene")
    public T getGene() {
        return null;
    }

    @Override
    @GraphQLQuery(name = "name", description = "A person's name")
    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public Collection<Address> getAddresses() {
        return addresses;
    }

    public String getTitle() {
        return title;
    }
}
