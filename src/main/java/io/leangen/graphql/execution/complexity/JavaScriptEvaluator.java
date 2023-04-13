package io.leangen.graphql.execution.complexity;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

public class JavaScriptEvaluator extends AbstractComplexityFunction {

    private final ScriptEngine engine;

    public JavaScriptEvaluator() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("JavaScript");
    }

    @Override
    protected int eval(String expression, Map<String, Object> arguments) throws ScriptException {
        Bindings bindings = engine.createBindings();
        bindings.putAll(arguments);
        return ((Number) engine.eval(expression, bindings)).intValue();
    }
}
