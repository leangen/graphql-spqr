package io.leangen.graphql.execution.complexity;

import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.GraphQLUtils;

import java.util.Collections;
import java.util.Map;

public class ResolvedField {

    private final String name;
    private final FieldCoordinates coordinates;
    private final Field field;
    private final GraphQLFieldDefinition fieldDefinition;
    private final GraphQLOutputType fieldType;
    private final Map<String, Object> arguments;
    private final Resolver resolver;

    private final Map<String, ResolvedField> children;
    private int complexityScore;

    public ResolvedField(FieldCoordinates coordinates, Field field, GraphQLFieldDefinition fieldDefinition, Map<String, Object> arguments, Resolver resolver) {
        this(coordinates, field, fieldDefinition, arguments, Collections.emptyMap(), resolver);
    }

    public ResolvedField(FieldCoordinates coordinates, Field field, GraphQLFieldDefinition fieldDefinition, Map<String, Object> arguments,
                         Map<String, ResolvedField> children, Resolver resolver) {
        this.name = field.getAlias() != null ? field.getAlias() : field.getName();
        this.coordinates = coordinates;
        this.field = field;
        this.fieldDefinition = fieldDefinition;
        this.fieldType = (GraphQLOutputType) GraphQLUtils.unwrap(fieldDefinition.getType());
        this.arguments = arguments;
        this.children = children;
        this.resolver = resolver;
    }

    public ResolvedField(Map<String, ResolvedField> children) {
        this.name = null;
        this.coordinates = null;
        this.field = null;
        this.fieldDefinition = null;
        this.fieldType = null;
        this.arguments = null;
        this.children = children;
        this.resolver = null;
        this.complexityScore = children.values().stream().mapToInt(ResolvedField::getComplexityScore).sum();
    }

    public String getName() {
        return name;
    }

    public FieldCoordinates getCoordinates() {
        return coordinates;
    }

    public Field getField() {
        return field;
    }

    public GraphQLFieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    public GraphQLOutputType getFieldType() {
        return fieldType;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public Map<String, ResolvedField> getChildren() {
        return children;
    }

    public int getComplexityScore() {
        return complexityScore;
    }

    public void setComplexityScore(int complexityScore) {
        this.complexityScore = complexityScore;
    }

    public Resolver getResolver() {
        return resolver;
    }

    @Override
    public String toString() {
        return name;
    }
}
