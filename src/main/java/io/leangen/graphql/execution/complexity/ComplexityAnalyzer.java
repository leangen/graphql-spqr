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

    public int complexity(ExecutionContext context) {
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer(maxComplexity, complexityFunction, typeRegistry);
        ExecutableNormalizedOperation tree = context.getNormalizedQueryTree().get();
        GraphQLSchema schema = context.getGraphQLSchema();

        int totalComplexity = tree.getTopLevelFields().stream()
                .map(root -> resolvedField(schema, root))
                .mapToInt(field -> analyzer.complexity(schema, field))
                .sum();
        return check(totalComplexity);
    }

    private int complexity(GraphQLSchema schema, ResolvedField resolvedField) {
        List<ExecutableNormalizedField> fields = resolvedField.getField().getChildren();
        if (isAbstract(resolvedField.getFieldType())) {
            List<ExecutableNormalizedField> unconditionalFields = new ArrayList<>();
            Map<String, List<ExecutableNormalizedField>> conditionalFieldsPerType = new HashMap<>();
            // List<MappedType> outputTypes = typeRegistry.getOutputTypes(resolvedField.getFieldType().getName());
            // 👆 can be used instead of the potentially expensive isConditional().
            // if subField.getObjectTypeNames().size() != outputTypes.size() -> the field is conditional
            for (ExecutableNormalizedField subField : fields) {
                if (subField.isConditional(schema)) {
                    subField.getObjectTypeNames().forEach(obj -> {
                        conditionalFieldsPerType.computeIfAbsent(obj, __ -> new ArrayList<>());
                        conditionalFieldsPerType.get(obj).add(subField);
                    });
                } else {
                    unconditionalFields.add(subField);
                }
            }
            int unconditionalChildScore = score(schema, unconditionalFields);
            int maxConditionalChildScore = conditionalFieldsPerType.entrySet().stream()
                    .mapToInt(e -> score(schema, e.getKey(), e.getValue()))
                    .max()
                    .orElse(0);
            return check(complexityFunction.getComplexity(resolvedField, unconditionalChildScore + maxConditionalChildScore));
        }
        int childScore = score(schema, fields);
        return check(complexityFunction.getComplexity(resolvedField, childScore));
    }

    private boolean isAbstract(GraphQLType type) {
        return type instanceof GraphQLInterfaceType || type instanceof GraphQLUnionType;
    }

    private FieldCoordinates coordinates(GraphQLSchema schema, ExecutableNormalizedField field) {
        return coordinates(GraphQLUtils.unwrap(field.getParent().getType(schema)).getName(), field);
    }

    private FieldCoordinates coordinates(String concreteTypeName, ExecutableNormalizedField field) {
        return FieldCoordinates.coordinates(concreteTypeName, field.getName());
    }

    private ResolvedField resolvedField(GraphQLSchema schema, ExecutableNormalizedField field) {
        FieldCoordinates coordinates = FieldCoordinates.coordinates(field.getSingleObjectTypeName(), field.getFieldName());
        GraphQLOutputType type = field.getFieldDefinitions(schema).get(0).getType();
        Resolver resolver = typeRegistry.getMappedResolver(coordinates, field.getResolvedArguments().keySet());
        return new ResolvedField(coordinates, resolver, type, field.getResolvedArguments(), field);
    }

    private ResolvedField resolvedField(GraphQLSchema schema, FieldCoordinates coordinates, ExecutableNormalizedField field) {
        Resolver resolver = typeRegistry.getMappedResolver(coordinates, field.getResolvedArguments().keySet());
        return new ResolvedField(coordinates, resolver, field.getType(schema), field.getResolvedArguments(), field);
    }

    private int score(GraphQLSchema schema, List<ExecutableNormalizedField> fields) {
        return fields.stream()
                .mapToInt(field -> complexity(schema, resolvedField(schema, coordinates(schema, field), field)))
                .sum();
    }

    private int score(GraphQLSchema schema, String concreteTypeName, List<ExecutableNormalizedField> fields) {
        return fields.stream()
                .mapToInt(field -> complexity(schema, resolvedField(schema, coordinates(concreteTypeName, field), field)))
                .sum();
    }

    private int check(int score) {
        if (score > maxComplexity) {
            throw new ComplexityLimitExceededException(score, maxComplexity);
        }
        return score;
    }
}

