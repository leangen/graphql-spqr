package io.leangen.graphql.support;

import graphql.ExecutionResult;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class QueryResultAssertions {

    private static final String TYPE_MISMATCH_TEMPLATE = "The type %s found at path \"%s\" does not match the expected type %s";

    public static void assertNoErrors(ExecutionResult result) {
        assertTrue("Query result contains unexpected errors: " + result.getErrors(), result.getErrors().isEmpty());
    }

    public static void assertValueAtPathEquals(Object expected, ExecutionResult result, String path) {
        Object actual = get(path, result);
        if (expected instanceof Number) {
            assertEquals(((Number) expected).doubleValue(), ((Number) actual).doubleValue(), 0.0001);
        } else {
            assertEquals(expected, actual);
        }
    }

    public static void assertTypeAtPathIs(Class<?> expected, ExecutionResult result, String path) {
        Class<?> actual = get(path, result).getClass();
        assertTrue(String.format(TYPE_MISMATCH_TEMPLATE, actual.getName(), path, expected.getName()), expected.isAssignableFrom(actual));
    }

    public static void assertValueAtPath(Predicate<Object> assertion, ExecutionResult result, String path) {
        assertTrue(assertion.test(get(path, result)));
    }

    private static Object get(String path, ExecutionResult result) {
        return get(new LinkedList<>(Arrays.asList(path.split("\\."))), result.getData());
    }

    @SuppressWarnings("unchecked")
    private static Object get(Queue<String> path, Object result) {
        if (path.isEmpty()) {
            return result;
        }
        String currentKey = path.remove();
        try {
            int index = Integer.parseInt(currentKey);
            return get(path, ((List<?>) result).get(index));
        } catch (NumberFormatException e) {
            return get(path, ((Map<String, Object>) result).get(currentKey));
        }
    }
}
