package io.leangen.graphql.generator;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.*;
import io.leangen.graphql.annotations.NonNull;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.generator.strategy.AbstractTypeGenerationStrategy;
import io.leangen.graphql.metadata.DomainType;
import io.leangen.graphql.metadata.Query;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.query.HintedTypeResolver;
import io.leangen.graphql.query.IdTypeMapper;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;

import java.lang.reflect.AnnotatedType;
import java.util.*;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by bojan.tomic on 3/2/16.
 */
public class QueryGenerator {

	private List<GraphQLFieldDefinition> queries;
	private List<GraphQLFieldDefinition> mutations;
	private GraphQLInterfaceType node;

	private static final String RELAY_ID = "id";

	public QueryGenerator(QuerySourceRepository querySourceRepository, BuildContext.TypeGenerationMode mode) {
		QueryRepository queryRepository = new QueryRepository(querySourceRepository);
		BuildContext buildContext = new BuildContext(mode, queryRepository);
		node = buildContext.relay.nodeInterface(new HintedTypeResolver(buildContext.typeRepository));
		this.queries = generateQueries(buildContext);
		this.mutations = generateMutations(buildContext);
	}

	private List<GraphQLFieldDefinition> generateQueries(BuildContext buildContext) {
		Collection<Query> rootQueries = buildContext.queryRepository.getRootQueries();

		List<GraphQLFieldDefinition> queries = new ArrayList<>(rootQueries.size() + 1);
		Map<String, Query> nodeQueriesByType = new HashMap<>();

		for (Query query : rootQueries) {
			GraphQLFieldDefinition graphQlQuery = toGraphQLQuery(query, null, new ArrayList<>(), buildContext);
			queries.add(graphQlQuery);

			nodeQueriesByType.put(graphQlQuery.getType().getName(), query);
		}

		//TODO Shouldn't this check if the return type has relayID? Also, why add queries without primary resolver?
		//Add support for Relay Node query only if Relay-enabled resolvers exist
		if (nodeQueriesByType.values().stream().filter(Query::hasPrimaryResolver).findFirst().isPresent()) {
			queries.add(buildContext.relay.nodeField(node, createNodeDataFetcher(nodeQueriesByType, buildContext.relay, buildContext.executionContext)));
		}
		return queries;
	}

	private List<GraphQLFieldDefinition> generateMutations(BuildContext buildContext) {
		Collection<Query> mutations = buildContext.queryRepository.getMutations();
		return mutations.stream()
				.map(mutation -> toGraphQLQuery(mutation, null, new ArrayList<>(), buildContext))
				.collect(Collectors.toList());
	}

	private GraphQLFieldDefinition toGraphQLQuery(Query query, String enclosingTypeName, List<String> parentTrail, BuildContext buildContext) {
		List<String> trail = new ArrayList<>(parentTrail);
		trail.add(query.getName());
		GraphQLFieldDefinition.Builder queryBuilder = newFieldDefinition()
				.name(query.getName())
				.description(query.getName())
				.type(toGraphQLType(query, trail, buildContext));
		addArguments(queryBuilder, query, trail, buildContext);
		if (query.getJavaType().isAnnotationPresent(RelayId.class)) {
			queryBuilder.dataFetcher(createRelayIdDataFetcher(query, enclosingTypeName, buildContext.relay, buildContext.idTypeMapper, buildContext.executionContext));
		} else {
			queryBuilder.dataFetcher(createDataFetcher(query, buildContext.executionContext));
		}

		return queryBuilder.build();
	}

	private GraphQLOutputType toGraphQLType(Query query, List<String> parentTrail, BuildContext buildContext) {
		GraphQLOutputType type = toGraphQLType(query.getJavaType(), parentTrail, buildContext);
		if (query.isPageable()) {
			GraphQLObjectType edge = buildContext.relay.edgeType(type.getName(), type, null, Collections.emptyList());
			type = buildContext.relay.connectionType(type.getName(), edge, Collections.emptyList());
		}
		return type;
	}

	private GraphQLOutputType toGraphQLType(AnnotatedType javaType, List<String> parentTrail, BuildContext buildContext) {
		javaType = ClassUtils.stripBounds(javaType);
		GraphQLOutputType type = toGraphQLTypeInner(javaType, parentTrail, buildContext);
		if (javaType.isAnnotationPresent(NonNull.class)) {
			return new GraphQLNonNull(type);
		}
		return type;
	}

	//TODO deal with maps here
	private GraphQLOutputType toGraphQLTypeInner(AnnotatedType javaType, List<String> parentTrail, BuildContext buildContext) {
		if (javaType.isAnnotationPresent(RelayId.class)) {
			return Scalars.GraphQLID;
		}
		if (GraphQLUtils.isScalar(javaType.getType())) {
			return GraphQLUtils.toGraphQLScalarType(javaType.getType());
		}
		if (ClassUtils.isSuperType(Collection.class, javaType.getType())) {
			return new GraphQLList(toGraphQLType(ClassUtils.getTypeArguments(javaType)[0], parentTrail, buildContext));
		}
		//Pages don't need special treatment here, just extract their real type
		if(ClassUtils.isSuperType(Page.class, javaType.getType())) {
			javaType = ClassUtils.getTypeArguments(javaType)[0];
		}
		DomainType domainType = new DomainType(javaType);
		return toGraphQLType(domainType, parentTrail, buildContext);
	}

	private GraphQLOutputType toGraphQLType(DomainType domainType, List<String> parentTrail, BuildContext buildContext) {

		Optional<Query> relayId = buildContext.queryRepository.getDomainQueries(parentTrail, domainType.getJavaType()).stream()
				.filter(query -> query.getJavaType().isAnnotationPresent(RelayId.class))
				.findFirst();

		if (relayId.isPresent()) {
			buildContext.proxyFactory.registerType(ClassUtils.getRawType(domainType.getJavaType().getType()));
		}

		AbstractTypeGenerationStrategy.Entry typeEntry = buildContext.typeStrategy.get(domainType, parentTrail);
		if (typeEntry.type.isPresent()) {
			return typeEntry.type.get();
		}

		GraphQLObjectType.Builder typeBuilder = newObject()
				.name(typeEntry.name)
				.description(domainType.getDescription());

		buildContext.queryRepository.getChildQueries(parentTrail, domainType.getJavaType()).stream()
				.filter(childQuery -> !childQuery.isVirtual())
				.forEach(childQuery -> typeBuilder.field(toGraphQLQuery(childQuery, typeEntry.name, new ArrayList<>(parentTrail), buildContext)));

		if (relayId.isPresent()) {
			typeBuilder.withInterface(node);
		}

		GraphQLObjectType type = typeBuilder.build();
		buildContext.typeRepository.registerType(domainType, type);
		return type;
	}

//	private GraphQLInterfaceType toGraphQLInterface(DomainType interfaze, List<String> parentTrail, BuildContext buildContext) {
//		GraphQLObjectType.Builder typeBuilder = newInterface()
//				.name(interfaze.getName())
//				.description(interfaze.getDescription())
//				.;
//	}

	private GraphQLInputType toGraphQLInputType(AnnotatedType javaType, List<String> parentTrail, BuildContext buildContext) {
		javaType = ClassUtils.stripBounds(javaType);
		GraphQLInputType type = toGraphQLInputTypeInner(javaType, parentTrail, buildContext);
		if (javaType.isAnnotationPresent(NonNull.class)) {
			return new GraphQLNonNull(type);
		}
		return type;
	}


	//TODO Deal with Maps here too, not only lists
	private GraphQLInputType toGraphQLInputTypeInner(AnnotatedType javaType, List<String> parentTrail, BuildContext buildContext) {
		if (javaType.isAnnotationPresent(RelayId.class)) {
			return Scalars.GraphQLID;
		}
		if (GraphQLUtils.isScalar(javaType.getType())) {
			return GraphQLUtils.toGraphQLScalarType(javaType.getType());
		}
		if (ClassUtils.isSuperType(Collection.class, javaType.getType())) {
			return new GraphQLList(toGraphQLInputType(ClassUtils.getTypeArguments(javaType)[0], parentTrail, buildContext));
		}
		Optional<GraphQLInputType> cached = buildContext.typeRepository.getInputType(javaType.getType());
		if (cached.isPresent()) {
			return cached.get();
		}
		//Pages don't need special treatment here, just extract their real type
		if(ClassUtils.isSuperType(Page.class, javaType.getType())) {
			javaType = ClassUtils.getTypeArguments(javaType)[0];
		}
		DomainType domainType = new DomainType(javaType);
		return toGraphQLInputType(domainType, parentTrail, buildContext);
	}

	private GraphQLInputType toGraphQLInputType(DomainType domainType, List<String> parentTrail, BuildContext buildContext) {
//		if (buildContext.inputsInProgress.contains(domainType.getInputName())) {
//			return new GraphQLTypeReference(domainType.getInputName());
//		}
		buildContext.inputsInProgress.add(domainType.getInputName());
		GraphQLInputObjectType.Builder typeBuilder = newInputObject()
				.name(domainType.getInputName())
				.description(domainType.getDescription());

		buildContext.queryRepository.getDomainQueries(parentTrail, domainType.getJavaType()).stream()
				.filter(query -> query.getArguments().size() == 0)
				.forEach(
						field -> typeBuilder.field(toGraphQLInputField(field, parentTrail, buildContext))
				);

		GraphQLInputObjectType type = typeBuilder.build();
		buildContext.typeRepository.registerType(domainType.getJavaType().getType(), type);
		return type;
	}

	private GraphQLInputObjectField toGraphQLInputField(Query query, List<String> parentTrail, BuildContext buildContext) {
		GraphQLInputObjectField.Builder builder = newInputObjectField()
				.name(query.getName())
				.description(query.getDescription())
				.type(toGraphQLInputType(query.getJavaType(), parentTrail, buildContext));
		return builder.build();
	}

	private void addArguments(GraphQLFieldDefinition.Builder queryBuilder, Query query, List<String> parentTrail, BuildContext buildContext) {
		query.getArguments()
				.forEach(argument -> queryBuilder.argument(newArgument() //attach each argument to the query
						.name(argument.getName())
						.description(argument.getDescription())
						.type(toGraphQLInputType(argument.getJavaType(), parentTrail, buildContext))
						.build()));
		if (query.isPageable()) {
			queryBuilder.argument(buildContext.relay.getConnectionFieldArguments());
		}
	}

	private DataFetcher createDataFetcher(Query query, ExecutionContext executionContext) {
		return env -> query.resolve(env, executionContext);
	}

	//TODO should this maybe just delegate?
	//e.g. return ((GraphQLObjectType)env.getGraphQLSchema().getType("")).getFieldDefinition("").getDataFetcher().get(env);
	private DataFetcher createNodeDataFetcher(Map<String, Query> nodeQueriesByType, Relay relay, ExecutionContext executionContext) {
		return env -> {
			String typeName;
			try {
				typeName = relay.fromGlobalId((String) env.getArguments().get(RELAY_ID)).type;
			} catch (Exception e) {
				throw new IllegalArgumentException(env.getArguments().get(RELAY_ID) + " is not a valid Relay node ID");
			}
			if (!nodeQueriesByType.containsKey(typeName)) {
				throw new IllegalArgumentException(typeName + " is not a known type");
			}
			if (!nodeQueriesByType.get(typeName).hasPrimaryResolver()) {
				throw new IllegalArgumentException("Query '" + nodeQueriesByType.get(typeName).getName() + "' has no primary resolver");
			}
			return nodeQueriesByType.get(typeName).resolve(env, executionContext);
		};
	}

	private DataFetcher createRelayIdDataFetcher(Query query, String enclosingTypeName, Relay relay, IdTypeMapper idTypeMapper, ExecutionContext executionContext) {
		return env -> {
			Object id = query.resolve(env, executionContext);
			return relay.toGlobalId(enclosingTypeName, idTypeMapper.serialize(id));
		};
	}

	public List<GraphQLFieldDefinition> getQueries() {
		return queries;
	}

	public List<GraphQLFieldDefinition> getMutations() {
		return mutations;
	}
}
