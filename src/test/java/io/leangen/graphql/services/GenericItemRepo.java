package io.leangen.graphql.services;

import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by bojan.tomic on 7/25/16.
 */
public class GenericItemRepo<T> {

    private Map<String, T> itemsByName = new HashMap<>();

    @Query
    public T getItem(String name) {
        return itemsByName.get(name);
    }

    @Query
    public Collection<? extends T> getAllItems() {
        return itemsByName.values();
    }

    @Query
    public T contains(@Argument(value = "item") T item) {
        return itemsByName.containsValue(item) ? item : null;
    }

    @Mutation
    public void addItem(@Argument(value = "name") String name, @Argument(value = "item") T item) {
        itemsByName.putIfAbsent(name, item);
    }

    @Mutation
    public void addItems(@Argument(value = "items") Set<T> items) {
        items.forEach(item -> itemsByName.putIfAbsent(item.toString(), item));
    }

    @Mutation
    public T removeItem(String name) {
        return itemsByName.remove(name);
    }

    @Mutation
    public Set<T> removeItems(Set<String> names) {
        Set<T> removed = new HashSet<>();
        names.forEach(name -> removed.add(itemsByName.remove(name)));
        return removed;
    }
}
