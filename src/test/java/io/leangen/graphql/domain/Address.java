package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bojan.tomic on 3/14/16.
 */
public class Address {

    private List<String> types = new ArrayList<>();
    private List<Street> streets = new ArrayList<>();

    @Query(value = "owner", description = "The landlord")
    public User<String> getOwner() {
        User<String> owner = new User<>();
        owner.id = 666;
        owner.name = "The Man (TM)";
        Address address1 = new Address();
        address1.setTypes(Collections.singletonList("creep"));
        Street street11 = new Street("Homestreet", 300);
        address1.getStreets().add(street11);
        owner.addresses = Collections.singleton(address1);
        return owner;
    }

    @Query(value = "streets", description = "The streets")
    public List<Street> getStreets() {
        return streets;
    }

    public void setStreets(List<Street> streets) {
        this.streets = streets;
    }

    @Query(value = "types", description = "Address types e.g. residential or office")
    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }
}
