package io.leangen.graphql.execution.complexity;

import graphql.execution.ExecutionContext;
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.ExecutableNormalizedOperation;
import graphql.schema.*;
import io.leangen.graphql.generator.TypeRegistry;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.GraphQLUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to perform static complexity analysis on the parsed operation AST.
 * It recursively walks the AST and accumulates the complexity scores.
 * Once the threshold is exceeded, it throws a {@link ComplexityLimitExceededException}.
 * The complexity score calculation for each node is delegated to {@link ComplexityFunction}.
 */
public class ComplexityAnalyzer {

    private final int maxComplexity;
    private final ComplexityFunction complexityFunction;
    private final TypeRegistry typeRegistry;

    public ComplexityAnalyzer(int maxComplexity, ComplexityFunction complexityFunction, TypeRegistry typeRegistry) {
        this.maxComplexity = maxComplexity;
        this.complexityFunction = complexityFunction;
        this.typeRegistry = typeRegistry;
    }

     /*
     ExecutableNormalizedOperation is graphql-java internal API.
     This method can hopefully be reimplemented to rely on supported API instead once
     https://github.com/graphql-java/graphql-java/pull/3174 is merged 🤞
     */
    public int complexity(ExecutionContext context) {
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer(maxComplexity, complexityFunction, typeRegistry);
        ExecutableNormalizedOperation tree = context.getNormalizedQueryTree().get();
        GraphQLSchema schema = context.getGraphQLSchema();

        int totalComplexity = tree.getTopLevelFields().stream()
                .map(root -> resolvedField(schema, root))
                .mapToInt(analyzer::complexity)
                .sum();
        return check(totalComplexity);
    }

    // This method exists solely to exemplify what can currently be done using only the public API.
    // Remove if the above-mentioned issue is resolved in graphql-java.
    private int complexity(DataFetchingEnvironment env) {
        return complexity(resolvedField(env));
    }

    private int complexity(ResolvedField resolvedField) {
        List<SelectedField> fields = resolvedField.getSelectionSet().getImmediateFields();
        if (isAbstract(resolvedField.getFieldType())) {
            List<SelectedField> unconditionalFields = new ArrayList<>();
            Map<String, List<SelectedField>> conditionalFieldsPerType = new HashMap<>();
            // List<MappedType> outputTypes = typeRegistry.getOutputTypes(resolvedField.getFieldType().getName());
            for (SelectedField subField : fields) {
                if (subField.isConditional()) {
                    subField.getObjectTypeNames().forEach(obj -> {
                        conditionalFieldsPerType.computeIfAbsent(obj, __ -> new ArrayList<>());
                        conditionalFieldsPerType.get(obj).add(subField);
                    });
                } else {
                    unconditionalFields.add(subField);
                }
            }
            int unconditionalChildScore = score(unconditionalFields);
            int maxConditionalChildScore = conditionalFieldsPerType.entrySet().stream()
                    .mapToInt(e -> score(e.getKey(), e.getValue()))
                    .max()
                    .orElse(0);
            return check(complexityFunction.getComplexity(resolvedField, unconditionalChildScore + maxConditionalChildScore));
        }
        int childScore = score(fields);
        return check(complexityFunction.getComplexity(resolvedField, childScore));
    }

    private boolean isAbstract(GraphQLType type) {
        return type instanceof GraphQLInterfaceType || type instanceof GraphQLUnionType;
    }

    private FieldCoordinates coordinates(DataFetchingEnvironment env) {
        return FieldCoordinates.coordinates(
                (GraphQLFieldsContainer) GraphQLUtils.unwrap(env.getParentType()), env.getFieldDefinition());
    }

    private FieldCoordinates coordinates(SelectedField field) {
        return coordinates(GraphQLUtils.unwrap(field.getParentField().getType()).getName(), field);
    }

    private FieldCoordinates coordinates(String concreteTypeName, SelectedField field) {
        return FieldCoordinates.coordinates(concreteTypeName, field.getName());
    }

    private ResolvedField resolvedField(DataFetchingEnvironment env) {
        FieldCoordinates coordinates = coordinates(env);
        Resolver resolver = typeRegistry.getMappedResolver(coordinates, env.getArguments().keySet());
        return new ResolvedField(coordinates, resolver,
                env.getFieldType(), env.getArguments(), env.getSelectionSet());
    }

    private ResolvedField resolvedField(GraphQLSchema schema, ExecutableNormalizedField field) {
        FieldCoordinates coordinates = FieldCoordinates.coordinates(field.getSingleObjectTypeName(), field.getFieldName());
        GraphQLOutputType type = field.getFieldDefinitions(schema).get(0).getType();
        DataFetchingFieldSelectionSet selectionSet = DataFetchingFieldSelectionSetImpl.newCollector(
                schema,
                type,
                () -> field);
        Resolver resolver = typeRegistry.getMappedResolver(coordinates, field.getResolvedArguments().keySet());
        return new ResolvedField(coordinates, resolver, type, field.getResolvedArguments(), selectionSet);
    }

    private ResolvedField resolvedField(FieldCoordinates coordinates, SelectedField field) {
        Resolver resolver = typeRegistry.getMappedResolver(coordinates, field.getArguments().keySet());
        return new ResolvedField(coordinates, resolver, field.getType(), field.getArguments(),
                field.getSelectionSet());
    }

    private int score(List<SelectedField> fields) {
        return fields.stream()
                .mapToInt(field -> complexity(resolvedField(coordinates(field), field)))
                .sum();
    }

    private int score(String concreteTypeName, List<SelectedField> fields) {
        return fields.stream()
                .mapToInt(field -> complexity(resolvedField(coordinates(concreteTypeName, field), field)))
                .sum();
    }

    private int check(int score) {
        if (score > maxComplexity) {
            throw new ComplexityLimitExceededException(score, maxComplexity);
        }
        return score;
    }
}

