package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLQuery;

/**
 * Created by bojan.tomic on 7/25/16.
 */
public class GenericItemHolder<T> {

	@GraphQLQuery(name = "item", description = "The item this holder contains")
	public T item;
	@GraphQLQuery(name = "name", description = "The name of the contained item")
	public String name;
}
