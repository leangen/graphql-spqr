package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLComplexity;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.Query;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/**
 * Created by bojan.tomic on 3/5/16.
 */
public class User<T> implements Person {

    public String name;

    @Query(value = "title", description = "A person's title")
    public String title;

    @GraphQLId(relayId = true)
    @Query(value = "id", description = "A person's id")
    public Integer id;

    @Query(value = "uuid", description = "A person's uuid")
    public UUID uuid;

    @Query(value = "regDate", description = "Date of registration")
    public Date registrationDate;

    @GraphQLComplexity("2 * childScore")
    @Query(value = "addresses", description = "A person's address")
    public Collection<Address> addresses;

    @Query(value = "gene")
    public T getGene() {
        return null;
    }

    @Override
    @Query(value = "name", description = "A person's name")
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

    @GraphQLComplexity("2 * childScore")
    public Collection<Address> getAddresses() {
        return addresses;
    }

    public String getTitle() {
        return title;
    }
}
