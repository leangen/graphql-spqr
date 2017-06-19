package io.leangen.graphql.support;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import io.leangen.graphql.execution.complexity.ComplexityLimitExceededException;

public class Matchers {
    
    public static ComplexityMatcher complexityScore(int expectedComplexity) {
        return new ComplexityMatcher(expectedComplexity);
    }

    private static class ComplexityMatcher extends BaseMatcher<ComplexityLimitExceededException> {

        private int actualComplexity;
        private final int expectedComplexity;

        ComplexityMatcher(int expectedComplexity) {
            this.expectedComplexity = expectedComplexity;
        }

        @Override
        public boolean matches(Object exception) {
            actualComplexity = ((ComplexityLimitExceededException) exception).getComplexity();
            return actualComplexity == expectedComplexity;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format(
                    "the complexity score of %d, but found %d", expectedComplexity, actualComplexity));
        }
    }
}
