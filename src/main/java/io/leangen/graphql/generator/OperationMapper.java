package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import graphql.schema.PropertyDataFetcher;
import io.leangen.graphql.GraphQLRuntime;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.OperationExecutor;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.OperationArgumentDefaultValue;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.GraphQLUtils;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * <p>Drives the work of mapping Java structures into their GraphQL representations.</p>
 * While the task of mapping types is delegated to instances of {@link io.leangen.graphql.generator.mapping.TypeMapper},
 * selection of mappers, construction and attachment of resolvers (modeled by {@link DataFetcher}s
 * in <a href=https://github.com/graphql-java/graphql-java>graphql-java</a>), and other universal tasks are encapsulated
 * in this class.
 */
public class OperationMapper {

    private List<GraphQLFieldDefinition> queries; //The list of all mapped queries
    private List<GraphQLFieldDefinition> mutations; //The list of all mapped mutations
    public GraphQLInterfaceType node; //Node interface, as defined by the Relay GraphQL spec

    /**
     *
     * @param buildContext The shared context containing all the global information needed for mapping
     */
    public OperationMapper(BuildContext buildContext) {
        this.node = buildContext.relay.nodeInterface(new RelayNodeTypeResolver(buildContext.typeRepository, buildContext.typeInfoGenerator));
        this.queries = generateQueries(buildContext);
        this.mutations = generateMutations(buildContext);
    }

    /**
     * Generates {@link GraphQLFieldDefinition}s representing all top-level queries (with their types, arguments and sub-queries)
     * fully mapped. This is the entry point into the query-mapping logic.
     *
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return A list of {@link GraphQLFieldDefinition}s representing all top-level queries
     */
    private List<GraphQLFieldDefinition> generateQueries(BuildContext buildContext) {
        List<Operation> rootQueries = new ArrayList<>(buildContext.operationRepository.getQueries());
        List<GraphQLFieldDefinition> queries = rootQueries.stream()
                .map(query -> toGraphQLOperation(query, buildContext))
                .collect(Collectors.toList());

        Map<String, String> nodeQueriesByType = getNodeQueriesByType(rootQueries, queries, buildContext.typeRepository);
        //Add support for Relay Node query only if Relay-enabled resolvers exist
        if (!nodeQueriesByType.isEmpty()) {
            queries.add(buildContext.relay.nodeField(node, createNodeResolver(nodeQueriesByType, buildContext.relay)));
        }
        return queries;
    }

    /**
     * Generates {@link GraphQLFieldDefinition}s representing all mutations (with their types, arguments and sub-queries)
     * fully mapped. By the GraphQL spec, all mutations are top-level only.
     * This is the entry point into the mutation-mapping logic.
     *
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return A list of {@link GraphQLFieldDefinition}s representing all mutations
     */
    private List<GraphQLFieldDefinition> generateMutations(BuildContext buildContext) {
        Collection<Operation> mutations = buildContext.operationRepository.getMutations();
        return mutations.stream()
                .map(mutation -> buildContext.relayMappingConfig.relayCompliantMutations
                        ? toRelayMutation(toGraphQLOperation(mutation, buildContext), buildContext.relayMappingConfig)
                        : toGraphQLOperation(mutation, buildContext))
                .collect(Collectors.toList());
    }

    /**
     * Maps a single operation to a GraphQL output field (as queries in GraphQL are nothing but fields of the root operation type).
     *
     * @param operation The operation to map to a GraphQL output field
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return GraphQL output field representing the given operation
     */
    public GraphQLFieldDefinition toGraphQLOperation(Operation operation, BuildContext buildContext) {
        Set<Type> abstractTypes = new HashSet<>();
        GraphQLOutputType type = toGraphQLType(operation.getJavaType(), abstractTypes, buildContext);
        GraphQLFieldDefinition.Builder queryBuilder = newFieldDefinition()
                .name(operation.getName())
                .description(operation.getName())
                .type(type);
        operation.getArguments().stream()
                .filter(OperationArgument::isMappable)
                .forEach(argument -> queryBuilder.argument(toGraphQLArgument(argument, abstractTypes, buildContext)));
        if (type.getName() != null && !type.getName().equals("Connection") && type.getName().endsWith("Connection")) {
            queryBuilder.argument(buildContext.relay.getConnectionFieldArguments());
        }
        ValueMapper valueMapper = buildContext.valueMapperFactory.getValueMapper(abstractTypes);
        queryBuilder.dataFetcher(createResolver(operation, valueMapper, buildContext.globalEnvironment));

        return queryBuilder.build();
    }

    /**
     * Maps a Java type to a GraphQL output type. Delegates most of the work to applicable
     * {@link io.leangen.graphql.generator.mapping.TypeMapper}s.
     * <p>See {@link TypeMapper#toGraphQLType(AnnotatedType, Set, OperationMapper, BuildContext)}</p>
     *
     * @param javaType The Java type that is to be mapped to a GraphQL output type
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return GraphQL output type corresponding to the given Java type
     */
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, BuildContext buildContext) {
        return buildContext.typeMappers.getTypeMapper(javaType).toGraphQLType(javaType, abstractTypes, this, buildContext);
    }

    /**
     * Maps a single field/property to a GraphQL input field.
     *
     * @param inputField The field/property to map to a GraphQL input field
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return GraphQL input field representing the given field/property
     */
    public GraphQLInputObjectField toGraphQLInputField(InputField inputField, Set<Type> abstractTypes, BuildContext buildContext) {
        GraphQLInputObjectField.Builder builder = newInputObjectField()
                .name(inputField.getName())
                .description(inputField.getDescription())
                .type(toGraphQLInputType(inputField.getJavaType(), abstractTypes, buildContext));
        return builder.build();
    }

    /**
     * Maps a Java type to a GraphQL input type. Delegates most of the work to applicable
     * {@link io.leangen.graphql.generator.mapping.TypeMapper}s.
     * <p>See {@link TypeMapper#toGraphQLInputType(AnnotatedType, Set, OperationMapper, BuildContext)}</p>
     *
     * @param javaType The Java type that is to be mapped to a GraphQL input type
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return GraphQL input type corresponding to the given Java type
     */
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, BuildContext buildContext) {
        TypeMapper mapper = buildContext.typeMappers.getTypeMapper(javaType);
        return mapper.toGraphQLInputType(javaType, abstractTypes, this, buildContext);
    }

    private GraphQLArgument toGraphQLArgument(OperationArgument operationArgument, Set<Type> abstractTypes, BuildContext buildContext) {
        Set<Type> argumentAbstractTypes = new HashSet<>();
        GraphQLArgument.Builder argument = newArgument()
                .name(operationArgument.getName())
                .description(operationArgument.getDescription())
                .type(toGraphQLInputType(operationArgument.getJavaType(), argumentAbstractTypes, buildContext));

        abstractTypes.addAll(argumentAbstractTypes);
        OperationArgumentDefaultValue defaultValue = operationArgument.getDefaultValue();
        if (defaultValue.isPresent()) {
            argument.defaultValue(defaultValue.get());
        }
        return argument.build();
    }

    private GraphQLFieldDefinition toRelayMutation(GraphQLFieldDefinition mutation, RelayMappingConfig relayMappingConfig) {
        
        List<GraphQLFieldDefinition> outputFields;
        if (mutation.getType() instanceof GraphQLObjectType) {
            outputFields = ((GraphQLObjectType) mutation.getType()).getFieldDefinitions();
        } else {
            outputFields = new ArrayList<>();
            outputFields.add(GraphQLFieldDefinition.newFieldDefinition()
                    .name(relayMappingConfig.wrapperFieldName)
                    .description(relayMappingConfig.wrapperFieldDescription)
                    .type(mutation.getType())
                    .dataFetcher(DataFetchingEnvironment::getSource)
                    .build());
        }
        List<GraphQLInputObjectField> inputFields = mutation.getArguments().stream()
                .map(arg -> GraphQLInputObjectField.newInputObjectField()
                        .name(arg.getName())
                        .description(arg.getDescription())
                        .type(arg.getType())
                        .defaultValue(arg.getDefaultValue())
                        .build())
                .collect(Collectors.toList());
        GraphQLInputObjectType inputObjectType = newInputObject()
                .name(mutation.getName() + "Input")
                .field(newInputObjectField()
                        .name("clientMutationId")
                        .type(new GraphQLNonNull(GraphQLString)))
                .fields(inputFields)
                .build();
        GraphQLObjectType outputType = newObject()
                .name(mutation.getName() + "Payload")
                .field(newFieldDefinition()
                        .name("clientMutationId")
                        .type(new GraphQLNonNull(GraphQLString))
                        .dataFetcher(env -> env.getContext() instanceof GraphQLRuntime.ContextWrapper
                                ? ((GraphQLRuntime.ContextWrapper) env.getContext()).getExtension("clientMutationId")
                                : new PropertyDataFetcher("clientMutationId")))
                .fields(outputFields)
                .build();

        return newFieldDefinition()
                .name(mutation.getName())
                .type(outputType)
                .argument(newArgument()
                        .name("input")
                        .type(new GraphQLNonNull(inputObjectType)))
                .dataFetcher(env -> {
                    Map<String, Object> input = (Map<String, Object>) env.getArguments().get("input");
                    env.getArguments().clear();
                    env.getArguments().putAll(input);
                    if (env.getContext() instanceof GraphQLRuntime.ContextWrapper) {
                        GraphQLRuntime.ContextWrapper context = env.getContext();
                        context.putExtension("clientMutationId", env.getArgument("clientMutationId"));
//                        if (!(mutation.getType() instanceof GraphQLObjectType)) {
//                            Object result = mutation.getDataFetcher().get(env);
//                            context.putExtension("result", result);
//                            return result;
//                        }
                    }
                    return mutation.getDataFetcher().get(env);
                })
                .build();
    }

    /**
     * Creates a generic resolver for the given operation.
     * @implSpec This resolver simply invokes {@link OperationExecutor#execute(DataFetchingEnvironment)}
     *
     * @param operation The operation for which the resolver is being created
     * @param valueMapper Mapper to be used to deserialize raw argument values
     * @param globalEnvironment The shared context containing all the global information needed for operation resolution
     *
     * @return The resolver for the given operation
     */
    private DataFetcher createResolver(Operation operation, ValueMapper valueMapper, GlobalEnvironment globalEnvironment) {
        return new OperationExecutor(operation, valueMapper, globalEnvironment)::execute;
    }

    /**
     * Creates a resolver for the <em>node</em> query as defined by the Relay GraphQL spec.
     * <p>This query only takes a singe argument called "id" of type String, and returns the object implementing the
     * <em>Node</em> interface to which the given id corresponds.</p>
     *
     * @param nodeQueriesByType A map of all queries whose return types implement the <em>Node</em> interface, keyed
     *                          by their corresponding GraphQL type name
     * @param relay Relay helper
     *
     * @return The node query resolver
     */
    private DataFetcher createNodeResolver(Map<String, String> nodeQueriesByType, Relay relay) {
        return env -> {
            String typeName;
            try {
                typeName = relay.fromGlobalId((String) env.getArguments().get(GraphQLId.RELAY_ID_FIELD_NAME)).getType();
            } catch (Exception e) {
                throw new IllegalArgumentException(env.getArguments().get(GraphQLId.RELAY_ID_FIELD_NAME) + " is not a valid Relay node ID");
            }
            if (!nodeQueriesByType.containsKey(typeName)) {
                throw new IllegalArgumentException(typeName + " is not a Relay node type or no registered query can fetch it by ID");
            }
            return env.getGraphQLSchema().getQueryType().getFieldDefinition(nodeQueriesByType.get(typeName)).getDataFetcher().get(env);
        };
    }

    private Map<String, String> getNodeQueriesByType(List<Operation> queries,
                                                     List<GraphQLFieldDefinition> graphQlQueries,
                                                     TypeRepository typeRepository) {

        Map<String, String> nodeQueriesByType = new HashMap<>();
        Map<String, String> directNodeQueriesByType = new HashMap<>();

        for (int i = 0; i < queries.size(); i++) {
            Operation query = queries.get(i);
            GraphQLFieldDefinition graphQlQuery = graphQlQueries.get(i);

            if (graphQlQuery.getArgument(GraphQLId.RELAY_ID_FIELD_NAME) != null
                    && GraphQLUtils.isRelayId(graphQlQuery.getArgument(GraphQLId.RELAY_ID_FIELD_NAME))
                    && query.getResolver(GraphQLId.RELAY_ID_FIELD_NAME) != null) {

                GraphQLType unwrappedQueryType = GraphQLUtils.unwrapNonNull(graphQlQuery.getType());
                if (unwrappedQueryType instanceof GraphQLObjectType
                        && ((GraphQLObjectType) unwrappedQueryType).getInterfaces().contains(this.node)) {
                    directNodeQueriesByType.put(unwrappedQueryType.getName(), query.getName());
                } else if (unwrappedQueryType instanceof GraphQLInterfaceType || unwrappedQueryType instanceof GraphQLUnionType) {
                    typeRepository.getOutputTypes(unwrappedQueryType.getName()).stream()
                            .map(mappedType -> mappedType.graphQLType)
                            .filter(implementation -> implementation.getInterfaces().contains(this.node))
                            .forEach(nodeType -> nodeQueriesByType.put(nodeType.getName(), query.getName()));
                }
            }
        }
        //this way more precise queries (returning node types directly) override interface/union queries
        nodeQueriesByType.putAll(directNodeQueriesByType);
        return nodeQueriesByType;
    }

    /**
     * Fetches all the mapped GraphQL fields representing top-level queries, ready to be attached to the root query type.
     *
     * @return A list of GraphQL fields representing top-level queries
     */
    public List<GraphQLFieldDefinition> getQueries() {
        return queries;
    }

    /**
     * Fetches all the mapped GraphQL fields representing mutations, ready to be attached to the root mutation type.
     *
     * @return A list of GraphQL fields representing mutations
     */
    public List<GraphQLFieldDefinition> getMutations() {
        return mutations;
    }
}
