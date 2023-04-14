package io.leangen.graphql.execution.complexity;

import java.util.Map;

public class SimpleComplexityFunction extends AbstractComplexityFunction {

    @Override
    protected int eval(String expression, Map<String, Object> arguments) {
        return Expressions.eval(expression, arguments).intValue();
    }
}
