package io.leangen.graphql.generator;

import graphql.relay.Relay;
import graphql.schema.*;
import io.leangen.graphql.annotations.NonNull;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.metadata.Query;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.query.HintedTypeResolver;
import io.leangen.graphql.query.IdTypeMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.*;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;

/**
 * Created by bojan.tomic on 3/2/16.
 */
public class QueryGenerator {

    private List<GraphQLFieldDefinition> queries;
    private List<GraphQLFieldDefinition> mutations;
    public GraphQLInterfaceType node;

    private static final String RELAY_ID = "id";

    public QueryGenerator(QuerySourceRepository querySourceRepository, TypeMapperRepository typeMappers, ConverterRepository converters, BuildContext.TypeGenerationMode mode) {
        QueryRepository queryRepository = new QueryRepository(querySourceRepository);
        BuildContext buildContext = new BuildContext(mode, queryRepository, typeMappers, converters);
        this.node = buildContext.relay.nodeInterface(new HintedTypeResolver(buildContext.typeRepository));
        this.queries = generateQueries(buildContext);
        this.mutations = generateMutations(buildContext);
    }

    private List<GraphQLFieldDefinition> generateQueries(BuildContext buildContext) {
        Collection<Query> rootQueries = buildContext.queryRepository.getRootQueries();

        List<GraphQLFieldDefinition> queries = new ArrayList<>(rootQueries.size() + 1);
        Map<String, Query> nodeQueriesByType = new HashMap<>();

        for (Query query : rootQueries) {
            GraphQLFieldDefinition graphQlQuery = toGraphQLQuery(query, null, buildContext);
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
                .map(mutation -> toGraphQLQuery(mutation, null, buildContext))
                .collect(Collectors.toList());
    }

    public GraphQLFieldDefinition toGraphQLQuery(Query query, String enclosingTypeName, BuildContext buildContext) {
        GraphQLFieldDefinition.Builder queryBuilder = newFieldDefinition()
                .name(query.getName())
                .description(query.getName())
                .type(toGraphQLType(query, buildContext));
        addArguments(queryBuilder, query, buildContext);
        if (query.getJavaType().isAnnotationPresent(RelayId.class)) {
            queryBuilder.dataFetcher(createRelayIdDataFetcher(query, enclosingTypeName, buildContext.relay, buildContext.idTypeMapper, buildContext.executionContext));
        } else {
            queryBuilder.dataFetcher(createDataFetcher(query, buildContext.executionContext));
        }

        return queryBuilder.build();
    }

    private GraphQLOutputType toGraphQLType(Query query, BuildContext buildContext) {
        GraphQLOutputType type = toGraphQLType(query.getJavaType(), buildContext);
        if (query.isPageable()) {
            GraphQLObjectType edge = buildContext.relay.edgeType(type.getName(), type, null, Collections.emptyList());
            type = buildContext.relay.connectionType(type.getName(), edge, Collections.emptyList());
        }
        return type;
    }

    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, BuildContext buildContext) {
        javaType = ClassUtils.stripBounds(javaType);
        GraphQLOutputType type = buildContext.typeMappers.getTypeMapper(javaType).toGraphQLType(javaType, buildContext, this);
        if (javaType.isAnnotationPresent(NonNull.class)) {
            return new GraphQLNonNull(type);
        }
        return type;
    }

//	private GraphQLInterfaceType toGraphQLInterface(DomainType interfaze, BuildContext buildContext) {
//		GraphQLObjectType.Builder typeBuilder = newInterface()
//				.name(interfaze.getName())
//				.description(interfaze.getDescription())
//				.;
//	}

    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, BuildContext buildContext) {
        javaType = ClassUtils.stripBounds(javaType);
        GraphQLInputType type = buildContext.typeMappers.getTypeMapper(javaType).toGraphQLInputType(javaType, buildContext, this);
        if (javaType.isAnnotationPresent(NonNull.class)) {
            return new GraphQLNonNull(type);
        }
        return type;
    }

    public GraphQLInputObjectField toGraphQLInputField(Query query, BuildContext buildContext) {
        GraphQLInputObjectField.Builder builder = newInputObjectField()
                .name(query.getName())
                .description(query.getDescription())
                .type(toGraphQLInputType(query.getJavaType(), buildContext));
        return builder.build();
    }

    private void addArguments(GraphQLFieldDefinition.Builder queryBuilder, Query query, BuildContext buildContext) {
        query.getArguments()
                .forEach(argument -> queryBuilder.argument(newArgument() //attach each argument to the query
                        .name(argument.getName())
                        .description(argument.getDescription())
                        .type(toGraphQLInputType(argument.getJavaType(), buildContext))
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
