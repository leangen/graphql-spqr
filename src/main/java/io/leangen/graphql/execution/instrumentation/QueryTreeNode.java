package io.leangen.graphql.execution.instrumentation;

import java.util.Collections;
import java.util.Map;

import graphql.language.Field;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.types.MappedGraphQLFieldDefinition;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.GraphQLUtils;

public class QueryTreeNode {

    private final String name;
    private final Field field;
    private final GraphQLFieldDefinition fieldDefinition;
    private final Map<String, Object> arguments;
    private final Resolver resolver;
    private final GraphQLOutputType fieldType;

    private Map<String, QueryTreeNode> children;
    private int complexityScore;

    public QueryTreeNode(Field field, GraphQLFieldDefinition fieldDefinition, Map<String, Object> arguments) {
        this(field, fieldDefinition, arguments, Collections.emptyMap());
    }
    
    public QueryTreeNode(Field field, GraphQLFieldDefinition fieldDefinition, Map<String, Object> arguments, Map<String, QueryTreeNode> children) {
        this.name = field.getAlias() != null ? field.getAlias() : field.getName();
        this.field = field;
        this.fieldDefinition = fieldDefinition;
        this.arguments = arguments;
        this.children = children;
        this.resolver = findResolver(fieldDefinition, arguments);
        this.fieldType = (GraphQLOutputType) GraphQLUtils.unwrap(fieldDefinition.getType());
    }
    
    private Resolver findResolver(GraphQLFieldDefinition fieldDefinition, Map<String, Object> arguments) {
        if (fieldDefinition instanceof MappedGraphQLFieldDefinition) {
            Operation operation = ((MappedGraphQLFieldDefinition) fieldDefinition).getOperation();
            if (operation.getResolvers().size() == 1) {
                return operation.getResolvers().iterator().next();
            } else {
                String[] nonNullArgumentNames = arguments.entrySet().stream()
                        .filter(arg -> arg.getValue() != null)
                        .map(Map.Entry::getKey)
                        .toArray(String[]::new);

                return operation.getResolver(nonNullArgumentNames);
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public Field getField() {
        return field;
    }

    public GraphQLFieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public Map<String, QueryTreeNode> getChildren() {
        return children;
    }

    public void setChildren(Map<String, QueryTreeNode> children) {
        this.children = children;
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

    public GraphQLOutputType getFieldType() {
        return fieldType;
    }

    @Override
    public String toString() {
        return name;
    }
}
