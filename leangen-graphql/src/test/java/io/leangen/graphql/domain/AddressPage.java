package io.leangen.graphql.domain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.leangen.graphql.query.relay.Edge;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.query.relay.PageInfo;
import io.leangen.graphql.query.relay.generic.GenericPageInfo;

/**
 * Created by bojan.tomic on 4/6/16.
 */
public class AddressPage implements Page<Address> {

	private List<Edge<Address>> edges;
	private boolean hasNext;

	public AddressPage(Collection<Address> addresses, boolean hasNext) {
		this.edges = addresses.stream().map(AddressEdge::new).collect(Collectors.toList());
		this.hasNext = hasNext;
	}

	@Override
	public Iterable<Edge<Address>> getEdges() {
		return edges;
	}

	@Override
	public PageInfo getPageInfo() {
		return new GenericPageInfo<>(edges.get(0).getCursor(), edges.get(edges.size() - 1).getCursor(), hasNext, true);
	}
}