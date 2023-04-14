import org.junit.Test;

import java.util.Map;

import static io.leangen.graphql.execution.complexity.Expressions.eval;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

public class ExpressionEvalTest {

    @Test
    public void trivialTest() {
        assertEquals(5, eval("5", emptyMap()).intValue());
    }

    @Test
    public void noVariablesInconsistentSpacesTest() {
        assertEquals(-48, eval("13 + 5*6 -7 * (8 +  5 )  ", emptyMap()).intValue());
    }

    @Test
    public void noVariablesLargeNumbersTest() {
        assertEquals(102, eval("(88888 / 11111 + 400 ) / (2 * 2)", emptyMap()).intValue());
    }

    @Test
    public void intVariablesTest() {
        assertEquals(-48, eval("x + word * 6 - 7 * (camelCase + 5)",
                Map.of("x", 13, "word", 5, "camelCase", 8)).intValue());
    }

    @Test
    public void stringVariablesTest() {
        assertEquals(-48, eval("x + word * 6 - 7 * (camelCase + 5)",
                Map.of("x", "13", "word", "5", "camelCase", "8")).intValue());
    }

    @Test
    public void mixedVariablesTest() {
        assertEquals(-48, eval("x + word * 6 - 7 * (camelCase + 5)",
                Map.of("x", 13, "word", "5", "camelCase", 8)).intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingOperatorTest() {
        eval("5 5", emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingVariableTest() {
        eval("5 * x", emptyMap());
    }
}
