package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;

import java.util.*;

/**
 * Created by bojan.tomic on 7/25/16.
 */
public class GenericItemRepo<T> {

    private Map<String, T> itemsByName = new HashMap<>();

    @GraphQLQuery
    public T getItem(String name) {
        return itemsByName.get(name);
    }

    @GraphQLQuery
    public Collection<? extends T> getAllItems() {
        return itemsByName.values();
    }

    @GraphQLQuery
    public T contains(@GraphQLArgument(name = "item") T item) {
        return itemsByName.containsValue(item) ? item : null;
    }

    @GraphQLMutation
    public void addItem(@GraphQLArgument(name = "name") String name, @GraphQLArgument(name = "item") T item) {
        itemsByName.putIfAbsent(name, item);
    }

    @GraphQLMutation
    public void addItems(@GraphQLArgument(name = "items") Set<T> items) {
        items.forEach(item -> itemsByName.putIfAbsent(item.toString(), item));
    }

    @GraphQLMutation
    public T removeItem(String name) {
        return itemsByName.remove(name);
    }

    @GraphQLMutation
    public Set<T> removeItems(Set<String> names) {
        Set<T> removed = new HashSet<>();
        names.forEach(name -> removed.add(itemsByName.remove(name)));
        return removed;
    }
}
