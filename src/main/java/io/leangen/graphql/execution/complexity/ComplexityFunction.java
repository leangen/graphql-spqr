package io.leangen.graphql.execution.complexity;

@FunctionalInterface
public interface ComplexityFunction {
    
    int getComplexity(ResolvedField field, int childScore);
}
