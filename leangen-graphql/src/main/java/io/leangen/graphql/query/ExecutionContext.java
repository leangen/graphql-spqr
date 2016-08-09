package io.leangen.graphql.query;

import java.lang.reflect.Type;

import graphql.relay.Relay;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLUnionType;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.generator.proxy.NodeTypeHintProvider;
import io.leangen.graphql.generator.proxy.ProxyFactory;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;

/**
 * Created by bojan.tomic on 5/14/16.
 */
public class ExecutionContext {

	public final Relay relay;
	public final TypeRepository typeRepository;
	public final ProxyFactory proxyFactory;
	public final IdTypeMapper idTypeMapper;
	public final InputDeserializer inputDeserializer;

	public ExecutionContext(Relay relay, TypeRepository typeRepository, ProxyFactory proxyFactory, IdTypeMapper idTypeMapper, InputDeserializer inputDeserializer) {
		this.relay = relay;
		this.typeRepository = typeRepository;
		this.proxyFactory = proxyFactory;
		this.idTypeMapper = idTypeMapper;
		this.inputDeserializer = inputDeserializer;
	}

	public Object proxyIfNeeded(DataFetchingEnvironment env, Object result, Type type) throws InstantiationException, IllegalAccessException {
		if (typeRepository.getOutputTypes(type).size() > 1
				&& (env.getFieldType() instanceof GraphQLInterfaceType || env.getFieldType() instanceof GraphQLUnionType)) {
			//TODO Should this check if the GraphQL type name (from ID) and Java type match?
			//The reason might be to throw a meaningful exception here before the one that would eventually be thrown downstream
			return proxyFactory.proxy(result, new NodeTypeHintProvider(result, env, relay));
		} else {
			return result;
		}
	}
}
