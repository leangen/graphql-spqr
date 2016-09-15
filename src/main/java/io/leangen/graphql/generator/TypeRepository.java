package io.leangen.graphql.generator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.graphql.metadata.DomainType;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class TypeRepository {

	private Map<String, GraphQLObjectType> outputTypesByName = new HashMap<>();
	private Map<Type, List<GraphQLObjectType>> outputTypesByClass = new HashMap<>();
	private Map<Type, GraphQLInputType> inputTypesByClass = new HashMap<>();

	public void registerType(DomainType domainType, GraphQLObjectType type) {
		this.outputTypesByName.put(type.getName(), type);
		this.outputTypesByClass.putIfAbsent(domainType.getJavaType().getType(), new ArrayList<>());
		this.outputTypesByClass.get(domainType.getJavaType().getType()).add(type);
	}

	public void registerType(Type javaType, GraphQLInputType type) {
		this.inputTypesByClass.put(javaType, type);
	}

	public GraphQLObjectType getOutputType(String name) {
		return outputTypesByName.get(name);
	}

	public Optional<GraphQLInputType> getInputType(Type javaType) {
		return Optional.ofNullable(inputTypesByClass.get(javaType));
	}

	public List<GraphQLObjectType> getOutputTypes(Type javaType) {
		return this.outputTypesByClass.entrySet().stream()
				.filter(entry -> GenericTypeReflector.isSuperType(entry.getKey(), javaType))
				.flatMap(entry -> entry.getValue().stream())
				.collect(Collectors.toList());
	}
}