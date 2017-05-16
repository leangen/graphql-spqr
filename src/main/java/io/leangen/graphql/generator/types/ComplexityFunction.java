package io.leangen.graphql.generator.types;

import java.util.Map;

@FunctionalInterface
public interface ComplexityFunction {
    
    Double calculateComplexity(Map<String, Object> arguments, Double childScore);
}
