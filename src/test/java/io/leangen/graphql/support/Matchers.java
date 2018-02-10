package io.leangen.graphql.support;

import io.leangen.graphql.execution.complexity.ComplexityLimitExceededException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class Matchers {
    
    public static ComplexityMatcher complexityScore(int expectedComplexity) {
        return new ComplexityMatcher(expectedComplexity);
    }

    private static class ComplexityMatcher extends BaseMatcher<ComplexityLimitExceededException> {

        private final int expectedComplexity;

        ComplexityMatcher(int expectedComplexity) {
            this.expectedComplexity = expectedComplexity;
        }

        @Override
        public boolean matches(Object exception) {
            return ((ComplexityLimitExceededException) exception).getComplexity() == expectedComplexity;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(ComplexityLimitExceededException.class.getSimpleName() +
                    " with the complexity score of " + expectedComplexity);
        }
    }
}
