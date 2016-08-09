package io.leangen.graphql.generator.proxy;

import graphql.relay.Relay;
import graphql.schema.DataFetchingEnvironment;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class NodeTypeHintProvider implements GraphQLTypeHintProvider {

	private Object result;
	private DataFetchingEnvironment env;
	private Relay relay;

	public NodeTypeHintProvider(Object result, DataFetchingEnvironment env, Relay relay) {
		this.result = result;
		this.env = env;
		this.relay = relay;
	}

	@Override
	public String getGraphQLTypeHint() {
		try {
			return relay.fromGlobalId(env.getArguments().get("id").toString()).type;
		} catch (Exception e) {
			throw new IllegalStateException("Indeterminable GraphQL type for class " + result.getClass(), e);
		}
	}
}