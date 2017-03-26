package io.leangen.graphql.domain;

import io.leangen.graphql.execution.relay.Edge;

/**
 * Created by bojan.tomic on 4/6/16.
 */
public class AddressEdge implements Edge<Address> {

    private String cursor;
    private Address node;

    AddressEdge(Address address) {
        this.cursor = address.toString();
        this.node = address;
    }

    @Override
    public String getCursor() {
        return cursor;
    }

    @Override
    public Address getNode() {
        return node;
    }
}
