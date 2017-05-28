package io.leangen.graphql.support;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import graphql.ExecutionResult;

import static org.junit.Assert.assertEquals;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class QueryResultAssertions {

    public static void assertValueAtPathEquals(Object expected, ExecutionResult result, String path) {
        assertEquals(expected, get(new LinkedList<>(Arrays.asList(path.split("\\."))), result.getData()));
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
