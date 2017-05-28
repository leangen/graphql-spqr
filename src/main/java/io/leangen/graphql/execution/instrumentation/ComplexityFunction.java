package io.leangen.graphql.execution.instrumentation;

@FunctionalInterface
public interface ComplexityFunction {
    
    int getComplexity(QueryTreeNode node, int childScore);
}
