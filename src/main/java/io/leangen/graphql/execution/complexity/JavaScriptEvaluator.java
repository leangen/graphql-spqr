package io.leangen.graphql.execution.complexity;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import io.leangen.graphql.util.GraphQLUtils;

public class JavaScriptEvaluator implements ComplexityFunction {
    
    private final ScriptEngine engine;

    public JavaScriptEvaluator() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("JavaScript");
    }

    @Override
    public int getComplexity(ResolvedField node, int childScore) {
        String expression = node.getResolver().getComplexityExpression();
        if (expression == null) {
            GraphQLType fieldType = GraphQLUtils.unwrap(node.getFieldDefinition().getType());
            if (fieldType instanceof GraphQLScalarType || fieldType instanceof GraphQLEnumType) {
                return 1;
            }
            return 1 + childScore;
        }
        Bindings bindings = engine.createBindings();
        bindings.putAll(node.getArguments());
        bindings.put("childScore", childScore);
        try {
            return ((Number) engine.eval(expression, bindings)).intValue();
        } catch (ScriptException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
