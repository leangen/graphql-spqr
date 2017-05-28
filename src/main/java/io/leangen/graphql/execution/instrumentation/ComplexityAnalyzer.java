package io.leangen.graphql.execution.instrumentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import graphql.execution.ConditionalNodes;
import graphql.execution.ExecutionContext;
import graphql.execution.FieldCollectorParameters;
import graphql.execution.ValuesResolver;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;

import static graphql.execution.TypeFromAST.getTypeFromAST;

/**
 * Class used to perform static complexity analysis on the parsed operation AST.
 * It recursively walks the AST and accumulates the complexity scores.
 * Once the threshold is exceeded, it throws a {@link ComplexityLimitExceededException}.
 * The complexity score calculation for each node is delegated to {@link ComplexityFunction}.
 */
class ComplexityAnalyzer {

    private final ConditionalNodes conditionalNodes;
    private final ComplexityFunction complexityFunction;
    private final int maximumComplexity;

    private static final ValuesResolver valuesResolver = new ValuesResolver();

    ComplexityAnalyzer(ComplexityFunction complexityFunction, int maximumComplexity) {
        this.conditionalNodes = new ConditionalNodes();
        this.complexityFunction = complexityFunction;
        this.maximumComplexity = maximumComplexity;
    }


    QueryTreeNode collectFields(ExecutionContext context) {
        FieldCollectorParameters parameters = FieldCollectorParameters.newParameters(context.getGraphQLSchema(), context.getGraphQLSchema().getQueryType())
                .fragments(context.getFragmentsByName())
                .variables(context.getVariables())
                .build();
        List<Field> fields = context.getOperationDefinition().getSelectionSet().getSelections().stream()
                .map(selection -> (Field) selection)
                .collect(Collectors.toList());
        Field field = fields.get(0);
        GraphQLFieldDefinition fieldDefinition = context.getGraphQLSchema().getQueryType().getFieldDefinition(field.getName());
        Map<String, Object> argumentValues = valuesResolver.getArgumentValues(fieldDefinition.getArguments(), field.getArguments(), context.getVariables());
        return collectFields(parameters, fields.stream().map(f -> new QueryTreeNode(f, fieldDefinition, argumentValues)).collect(Collectors.toList()));
    }

    /**
     * Given a list of fields this will collect the sub-field selections and return it as a map
     *
     * @param parameters the parameters to this method
     * @param fields     the list of fields to collect for
     *
     * @return a map of the sub field selections
     */
    private Map<String, QueryTreeNode> collectFields(FieldCollectorParameters parameters, List<Field> fields, GraphQLFieldsContainer parent) {
        List<String> visitedFragments = new ArrayList<>();
        Map<String, List<QueryTreeNode>> unconditionalSubFields = new LinkedHashMap<>();
        Map<String, Map<String, List<QueryTreeNode>>> conditionalSubFields = new LinkedHashMap<>();

        fields.stream()
                .filter(field -> field.getSelectionSet() != null)
                .forEach(field -> collectFields(parameters, unconditionalSubFields, getUnconditionalSelections(field.getSelectionSet()), visitedFragments, parent));

        fields.stream()
                .filter(field -> field.getSelectionSet() != null)
                .forEach(field ->
                        getConditionalSelections(field.getSelectionSet()).forEach((condition, selections) -> {
                                    Map<String, List<QueryTreeNode>> subFields = new LinkedHashMap<>();
                                    collectFields(parameters, subFields, selections, visitedFragments, parent);
                                    conditionalSubFields.put(condition, subFields);
                                }
                        ));

        if (conditionalSubFields.isEmpty()) {
            return unconditionalSubFields.values().stream()
                    .map(nodes -> collectFields(parameters, nodes))
                    .collect(Collectors.toMap(QueryTreeNode::getName, Function.identity()));
        } else {
            return reduceAlternatives(parameters, unconditionalSubFields, conditionalSubFields);
        }
    }

    private QueryTreeNode collectFields(FieldCollectorParameters parameters, List<QueryTreeNode> fields) {
        QueryTreeNode field = fields.get(0);
        if (!fields.stream().allMatch(f -> f.getFieldType() instanceof GraphQLFieldsContainer)) {
            field.setComplexityScore(complexityFunction.getComplexity(field, 0));
            return field;
        }
        List<Field> rawFields = fields.stream().map(QueryTreeNode::getField).collect(Collectors.toList());
        Map<String, QueryTreeNode> children = collectFields(parameters, rawFields, (GraphQLFieldsContainer) field.getFieldType());
        QueryTreeNode node = new QueryTreeNode(field.getField(), field.getFieldDefinition(), field.getArguments(), children);
        int childScore = children.values().stream().mapToInt(QueryTreeNode::getComplexityScore).sum();
        int complexityScore = complexityFunction.getComplexity(node, childScore);
        if (complexityScore > maximumComplexity) {
            throw new ComplexityLimitExceededException(complexityScore, maximumComplexity);
        }
        node.setComplexityScore(complexityScore);
        return node;
    }

    private void collectFields(FieldCollectorParameters parameters, Map<String, List<QueryTreeNode>> fields, List<Selection> selectionSet, List<String> visitedFragments, GraphQLFieldsContainer parent) {

        for (Selection selection : selectionSet) {
            if (selection instanceof Field) {
                collectField(parameters, fields, (Field) selection, parent);
            } else if (selection instanceof InlineFragment) {
                collectInlineFragment(parameters, fields, visitedFragments, (InlineFragment) selection, parent);
            } else if (selection instanceof FragmentSpread) {
                collectFragmentSpread(parameters, fields, visitedFragments, (FragmentSpread) selection, parent);
            }
        }
    }

    private void collectFragmentSpread(FieldCollectorParameters parameters, Map<String, List<QueryTreeNode>> fields, List<String> visitedFragments, FragmentSpread fragmentSpread, GraphQLFieldsContainer parent) {
        if (visitedFragments.contains(fragmentSpread.getName())) {
            return;
        }
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), fragmentSpread.getDirectives())) {
            return;
        }
        visitedFragments.add(fragmentSpread.getName());
        FragmentDefinition fragmentDefinition = parameters.getFragmentsByName().get(fragmentSpread.getName());

        if (!conditionalNodes.shouldInclude(parameters.getVariables(), fragmentDefinition.getDirectives())) {
            return;
        }
        if (fragmentDefinition.getTypeCondition() != null) {
            parent = (GraphQLFieldsContainer) getTypeFromAST(parameters.getGraphQLSchema(), fragmentDefinition.getTypeCondition());
        }
        collectFields(parameters, fields, fragmentDefinition.getSelectionSet().getSelections(), visitedFragments, parent);
    }

    private void collectInlineFragment(FieldCollectorParameters parameters, Map<String, List<QueryTreeNode>> fields, List<String> visitedFragments, InlineFragment inlineFragment, GraphQLFieldsContainer parent) {
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), inlineFragment.getDirectives())) {
            return;
        }
        if (inlineFragment.getTypeCondition() != null) {
            parent = (GraphQLFieldsContainer) getTypeFromAST(parameters.getGraphQLSchema(), inlineFragment.getTypeCondition());
        }
        collectFields(parameters, fields, inlineFragment.getSelectionSet().getSelections(), visitedFragments, parent);
    }

    private void collectField(FieldCollectorParameters parameters, Map<String, List<QueryTreeNode>> fields, Field field, GraphQLFieldsContainer parent) {
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), field.getDirectives())) {
            return;
        }
        GraphQLFieldDefinition fieldDefinition = parent.getFieldDefinition(field.getName());
        Map<String, Object> argumentValues = valuesResolver.getArgumentValues(fieldDefinition.getArguments(), field.getArguments(), parameters.getVariables());
        QueryTreeNode node = new QueryTreeNode(field, fieldDefinition, argumentValues);
        fields.putIfAbsent(node.getName(), new ArrayList<>());
        fields.get(node.getName()).add(node);
    }

    @SuppressWarnings("WeakerAccess")
    protected Map<String, QueryTreeNode> reduceAlternatives(FieldCollectorParameters parameters,
                                                            Map<String, List<QueryTreeNode>> unconditionalSubFields,
                                                            Map<String, Map<String, List<QueryTreeNode>>> conditionalSubFields) {
        Map<String, QueryTreeNode> reduced = null;
        for (Map.Entry<String, Map<String, List<QueryTreeNode>>> conditional : conditionalSubFields.entrySet()) {
            Map<String, List<QueryTreeNode>> merged = new HashMap<>(conditional.getValue());
            for (Map.Entry<String, List<QueryTreeNode>> unconditional : unconditionalSubFields.entrySet()) {
                merged.merge(unconditional.getKey(), unconditional.getValue(), (condNodes, uncondNodes) -> Stream.concat(condNodes.stream(), uncondNodes.stream()).collect(Collectors.toList()));
            }
            Map<String, QueryTreeNode> flat = merged.values().stream()
                    .map(nodes -> collectFields(parameters, nodes))
                    .collect(Collectors.toMap(QueryTreeNode::getName, Function.identity()));
            if (reduced == null) {
                reduced = flat;
            } else {
                int currentScore = flat.values().stream().mapToInt(QueryTreeNode::getComplexityScore).sum();
                int maxScore = reduced.values().stream().mapToInt(QueryTreeNode::getComplexityScore).sum();
                if (currentScore > maxScore) {
                    reduced = flat;
                }
            }
        }
        return reduced;
    }

    private List<Selection> getUnconditionalSelections(SelectionSet selectionSet) {
        return selectionSet.getSelections().stream()
                .filter(selection -> !isConditional(selection))
                .collect(Collectors.toList());
    }

    private Map<String, List<Selection>> getConditionalSelections(SelectionSet selectionSet) {
        return selectionSet.getSelections().stream()
                .filter(this::isConditional)
                .collect(Collectors.groupingBy(s -> s instanceof FragmentDefinition
                        ? ((FragmentDefinition) s).getTypeCondition().getName()
                        : ((InlineFragment) s).getTypeCondition().getName()));
    }
    
    private boolean isConditional(Selection selection) {
        return (selection instanceof FragmentDefinition && ((FragmentDefinition) selection).getTypeCondition() != null)
                || (selection instanceof InlineFragment && ((InlineFragment) selection).getTypeCondition() != null);
    }
}

