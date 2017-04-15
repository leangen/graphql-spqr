package io.leangen.graphql.execution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.execution.ExecutionContext;
import graphql.execution.FieldCollector;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLObjectType;

/**
 * Created by bojan.tomic on 4/15/17.
 */
public class FieldNameCollector {
    
    private final ExecutionContext executionContext;
    private static final FieldCollector fieldCollector = new FieldCollector();

    FieldNameCollector(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }
 
    public Set<String> getSelectedFieldNames(GraphQLObjectType objectType, SelectionSet selectionSet) {
        Map<String, List<Field>> subFields = new LinkedHashMap<>();
        List<String> visitedFragments = new ArrayList<>();
        fieldCollector.collectFields(executionContext, objectType, selectionSet, visitedFragments, subFields);
        return subFields.keySet();
    }
}
