package io.leangen.graphql.execution.complexity;

public class ComplexityLimitExceededException extends RuntimeException {

    private final int complexity;
    private final int maximumComplexity;

    public ComplexityLimitExceededException(int complexity, int maximumComplexity) {
        super("Requested operation exceeds the permitted complexity limit: " + complexity + " > " + maximumComplexity);
        this.complexity = complexity;
        this.maximumComplexity = maximumComplexity;
    }

    public int getComplexity() {
        return complexity;
    }

    public int getMaximumComplexity() {
        return maximumComplexity;
    }
}
