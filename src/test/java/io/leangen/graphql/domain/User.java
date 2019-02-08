package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLComplexity;
import io.leangen.graphql.annotations.GraphQLId;
import org.eclipse.microprofile.graphql.Query;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/**
 * Created by bojan.tomic on 3/5/16.
 */
public class User<T> implements Person {

    public String name;
    public String title;
    public Integer id;
    public UUID uuid;
    public Date registrationDate;
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

    @GraphQLId(relayId = true)
    @Query(value = "id", description = "A person's id")
    public Integer getId() {
        return id;
    }

    @Query(value = "uuid", description = "A person's uuid")
    public UUID getUuid() {
        return uuid;
    }

    @Query(value = "regDate", description = "Date of registration")
    public Date getRegistrationDate() {
        return registrationDate;
    }

    @GraphQLComplexity("2 * childScore")
    @Query(value = "addresses", description = "A person's address")
    public Collection<Address> getAddresses() {
        return addresses;
    }

    @Query(value = "title", description = "A person's title")
    public String getTitle() {
        return title;
    }
}
