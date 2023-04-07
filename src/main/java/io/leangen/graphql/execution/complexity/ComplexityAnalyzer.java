package io.leangen.graphql.execution.complexity;

import graphql.execution.CoercedVariables;
import graphql.execution.ConditionalNodes;
import graphql.execution.ExecutionContext;
import graphql.execution.FieldCollectorParameters;
import graphql.execution.ValuesResolver;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.generator.TypeRegistry;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.GraphQLUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.execution.TypeFromAST.getTypeFromAST;

/**
 * Class used to perform static complexity analysis on the parsed operation AST.
 * It recursively walks the AST and accumulates the complexity scores.
 * Once the threshold is exceeded, it throws a {@link ComplexityLimitExceededException}.
 * The complexity score calculation for each node is delegated to {@link ComplexityFunction}.
 */
public class ComplexityAnalyzer {

    private final ConditionalNodes conditionalNodes;
    private final ComplexityFunction complexityFunction;
    private final int maximumComplexity;
    private final TypeRegistry typeRegistry;

    public ComplexityAnalyzer(ComplexityFunction complexityFunction, int maximumComplexity, TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
        this.conditionalNodes = new ConditionalNodes();
        this.complexityFunction = complexityFunction;
        this.maximumComplexity = maximumComplexity;
    }


    public ResolvedField collectFields(ExecutionContext context) {
        FieldCollectorParameters parameters = FieldCollectorParameters.newParameters()
                .schema(context.getGraphQLSchema())
                .objectType(context.getGraphQLSchema().getQueryType())
                .fragments(context.getFragmentsByName())
                .variables(context.getCoercedVariables().toMap())
                .build();
        List<Field> fields = context.getOperationDefinition().getSelectionSet().getSelections().stream()
                .map(selection -> (Field) selection)
                .collect(Collectors.toList());

        Map<String, ResolvedField> roots = fields.stream()
                .map(field -> {
                    GraphQLFieldDefinition fieldDefinition;
                    FieldCoordinates fieldCoordinates;
                    if (GraphQLUtils.isIntrospectionField(field)) {
                        fieldDefinition = Introspection.SchemaMetaFieldDef;
                        fieldCoordinates = FieldCoordinates.systemCoordinates(field.getName());
                    } else {
                        GraphQLObjectType rootType = getRootType(context.getGraphQLSchema(), context.getOperationDefinition());
                        fieldDefinition = Objects.requireNonNull(rootType.getFieldDefinition(field.getName()));
                        fieldCoordinates = FieldCoordinates.coordinates(rootType, fieldDefinition);
                    }

                    Map<String, Object> argumentValues = ValuesResolver.getArgumentValues(fieldDefinition.getArguments(),
                            field.getArguments(), context.getCoercedVariables(), context.getGraphQLContext(), context.getLocale());
                    return collectFields(parameters, Collections.singletonList(
                            new ResolvedField(fieldCoordinates, field, fieldDefinition, argumentValues, findResolver(fieldCoordinates, argumentValues))), context);
                })
                .collect(Collectors.toMap(ResolvedField::getName, Function.identity()));

        ResolvedField root = new ResolvedField(roots);
        if (root.getComplexityScore() > maximumComplexity) {
            throw new ComplexityLimitExceededException(root.getComplexityScore(), maximumComplexity);
        }
        return root;
    }

    /**
     * Given a list of fields this will collect the sub-field selections and return it as a map
     *
     * @param parameters the parameters to this method
     * @param fields     the list of fields to collect for
     *
     * @return a map of the sub field selections
     */
    private Map<String, ResolvedField> collectFields(FieldCollectorParameters parameters, List<Field> fields, GraphQLFieldsContainer parent, ExecutionContext ctx) {
        List<String> visitedFragments = new ArrayList<>();
        Map<String, List<ResolvedField>> unconditionalSubFields = new LinkedHashMap<>();
        Map<String, Map<String, List<ResolvedField>>> conditionalSubFields = new LinkedHashMap<>();

        fields.stream()
                .filter(field -> field.getSelectionSet() != null)
                .forEach(field -> collectFields(parameters, unconditionalSubFields,
                        getUnconditionalSelections(field.getSelectionSet(), parameters), visitedFragments, parent, ctx));

        fields.stream()
                .filter(field -> field.getSelectionSet() != null)
                .forEach(field ->
                        getConditionalSelections(field.getSelectionSet(), parameters).forEach((condition, selections) -> {
                                    Map<String, List<ResolvedField>> subFields = new LinkedHashMap<>();
                                    collectFields(parameters, subFields, selections, visitedFragments, parent, ctx);
                                    conditionalSubFields.put(condition, subFields);
                                }
                        ));

        if (conditionalSubFields.isEmpty()) {
            return unconditionalSubFields.values().stream()
                    .map(nodes -> collectFields(parameters, nodes, ctx))
                    .collect(Collectors.toMap(ResolvedField::getName, Function.identity()));
        } else {
            return reduceAlternatives(parameters, ctx, unconditionalSubFields, conditionalSubFields);
        }
    }

    private ResolvedField collectFields(FieldCollectorParameters parameters, List<ResolvedField> fields, ExecutionContext ctx) {
        ResolvedField field = fields.get(0);
        if (!fields.stream().allMatch(f -> f.getFieldType() instanceof GraphQLFieldsContainer)) {
            field.setComplexityScore(complexityFunction.getComplexity(field, 0));
            return field;
        }
        List<Field> rawFields = fields.stream().map(ResolvedField::getField).collect(Collectors.toList());
        Map<String, ResolvedField> children = collectFields(parameters, rawFields, (GraphQLFieldsContainer) field.getFieldType(), ctx);
        Resolver resolver = findResolver(field.getCoordinates(), field.getArguments());
        ResolvedField node = new ResolvedField(field.getCoordinates(), field.getField(), field.getFieldDefinition(), field.getArguments(), children, resolver);
        int childScore = children.values().stream().mapToInt(ResolvedField::getComplexityScore).sum();
        int complexityScore = complexityFunction.getComplexity(node, childScore);
        if (complexityScore > maximumComplexity) {
            throw new ComplexityLimitExceededException(complexityScore, maximumComplexity);
        }
        node.setComplexityScore(complexityScore);
        return node;
    }

    private void collectFields(FieldCollectorParameters parameters, Map<String, List<ResolvedField>> fields, List<Selection> selectionSet,
                               List<String> visitedFragments, GraphQLFieldsContainer parent, ExecutionContext ctx) {

        for (Selection selection : selectionSet) {
            if (selection instanceof Field) {
                collectField(parameters, fields, (Field) selection, parent, ctx);
            } else if (selection instanceof InlineFragment) {
                collectInlineFragment(parameters, ctx, fields, visitedFragments, (InlineFragment) selection, parent);
            } else if (selection instanceof FragmentSpread) {
                collectFragmentSpread(parameters, ctx, fields, visitedFragments, (FragmentSpread) selection, parent);
            }
        }
    }

    private void collectFragmentSpread(FieldCollectorParameters parameters, ExecutionContext ctx, Map<String, List<ResolvedField>> fields,
                                       List<String> visitedFragments, FragmentSpread fragmentSpread, GraphQLFieldsContainer parent) {

        if (visitedFragments.contains(fragmentSpread.getName())) {
            return;
        }
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), fragmentSpread.getDirectives())) {
            return;
        }
        visitedFragments.add(fragmentSpread.getName());
        FragmentDefinition fragmentDefinition = definition(fragmentSpread, parameters);

        if (!conditionalNodes.shouldInclude(parameters.getVariables(), fragmentDefinition.getDirectives())) {
            return;
        }
        if (fragmentDefinition.getTypeCondition() != null) {
            parent = (GraphQLFieldsContainer) getTypeFromAST(parameters.getGraphQLSchema(), fragmentDefinition.getTypeCondition());
        }
        collectFields(parameters, fields, fragmentDefinition.getSelectionSet().getSelections(), visitedFragments, parent, ctx);
    }

    private void collectInlineFragment(FieldCollectorParameters parameters, ExecutionContext ctx, Map<String, List<ResolvedField>> fields,
                                       List<String> visitedFragments, InlineFragment inlineFragment, GraphQLFieldsContainer parent) {

        if (!conditionalNodes.shouldInclude(parameters.getVariables(), inlineFragment.getDirectives())) {
            return;
        }
        if (inlineFragment.getTypeCondition() != null) {
            parent = (GraphQLFieldsContainer) getTypeFromAST(parameters.getGraphQLSchema(), inlineFragment.getTypeCondition());
        }
        collectFields(parameters, fields, inlineFragment.getSelectionSet().getSelections(), visitedFragments, parent, ctx);
    }

    private void collectField(FieldCollectorParameters parameters, Map<String, List<ResolvedField>> fields, Field field, GraphQLFieldsContainer parent, ExecutionContext ctx) {
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), field.getDirectives())) {
            return;
        }
        GraphQLFieldDefinition fieldDefinition = parent.getFieldDefinition(field.getName());
        FieldCoordinates fieldCoordinates = FieldCoordinates.coordinates(parent, fieldDefinition);
        Map<String, Object> argumentValues = ValuesResolver.getArgumentValues(fieldDefinition.getArguments(), field.getArguments(),
                new CoercedVariables(parameters.getVariables()), ctx.getGraphQLContext(), ctx.getLocale());
        ResolvedField node = new ResolvedField(fieldCoordinates, field, fieldDefinition, argumentValues, findResolver(fieldCoordinates, argumentValues));
        fields.putIfAbsent(node.getName(), new ArrayList<>());
        fields.get(node.getName()).add(node);
    }

    @SuppressWarnings("WeakerAccess")
    protected Map<String, ResolvedField> reduceAlternatives(FieldCollectorParameters parameters, ExecutionContext ctx,
                                                            Map<String, List<ResolvedField>> unconditionalSubFields,
                                                            Map<String, Map<String, List<ResolvedField>>> conditionalSubFields) {
        Map<String, ResolvedField> reduced = null;
        for (Map.Entry<String, Map<String, List<ResolvedField>>> conditional : conditionalSubFields.entrySet()) {
            Map<String, List<ResolvedField>> merged = new HashMap<>(conditional.getValue());
            for (Map.Entry<String, List<ResolvedField>> unconditional : unconditionalSubFields.entrySet()) {
                merged.merge(unconditional.getKey(), unconditional.getValue(),
                        (condNodes, uncondNodes) -> Stream.concat(condNodes.stream(), uncondNodes.stream()).collect(Collectors.toList()));
            }
            Map<String, ResolvedField> flat = merged.values().stream()
                    .map(nodes -> collectFields(parameters, nodes, ctx))
                    .collect(Collectors.toMap(ResolvedField::getName, Function.identity()));
            if (reduced == null) {
                reduced = flat;
            } else {
                int currentScore = flat.values().stream().mapToInt(ResolvedField::getComplexityScore).sum();
                int maxScore = reduced.values().stream().mapToInt(ResolvedField::getComplexityScore).sum();
                if (currentScore > maxScore) {
                    reduced = flat;
                }
            }
        }
        return reduced;
    }

    private List<Selection> getUnconditionalSelections(SelectionSet selectionSet, FieldCollectorParameters parameters) {
        return selectionSet.getSelections().stream()
                .filter(selection -> !isConditional(selection, parameters))
                .collect(Collectors.toList());
    }

    private Map<String, List<Selection>> getConditionalSelections(SelectionSet selectionSet, FieldCollectorParameters parameters) {
        return selectionSet.getSelections().stream()
                .filter(selection -> isConditional(selection, parameters))
                .collect(Collectors.groupingBy(s -> s instanceof FragmentSpread
                        ? definition(s, parameters).getTypeCondition().getName()
                        : ((InlineFragment) s).getTypeCondition().getName()));
    }

    private boolean isConditional(Selection selection, FieldCollectorParameters parameters) {
        return (selection instanceof FragmentSpread && definition(selection, parameters).getTypeCondition() != null)
                || (selection instanceof InlineFragment && ((InlineFragment) selection).getTypeCondition() != null);
    }

    private GraphQLObjectType getRootType(GraphQLSchema schema, OperationDefinition operationDefinition) {
        if (operationDefinition.getOperation() == OperationDefinition.Operation.MUTATION) {
            return Objects.requireNonNull(schema.getMutationType());
        } else if (operationDefinition.getOperation() == OperationDefinition.Operation.QUERY) {
            return Objects.requireNonNull(schema.getQueryType());
        } else if (operationDefinition.getOperation() == OperationDefinition.Operation.SUBSCRIPTION) {
            return Objects.requireNonNull(schema.getSubscriptionType());
        } else {
            throw new IllegalStateException("Unknown operation type encountered. Incompatible graphql-java version?");
        }
    }

    private FragmentDefinition definition(Selection fragmentSpread, FieldCollectorParameters parameters) {
        return parameters.getFragmentsByName().get(((FragmentSpread) fragmentSpread).getName());
    }

    private Resolver findResolver(FieldCoordinates coordinates, Map<String, Object> arguments) {
        Operation mappedOperation = typeRegistry.getMappedOperation(coordinates);
        return mappedOperation != null ? mappedOperation.getApplicableResolver(arguments.keySet()) : null;
    }
}

