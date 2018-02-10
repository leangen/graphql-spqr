package io.leangen.graphql.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A collection of utility methods
 */
public class Utils {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Optional<T> or(Optional<T> left, Optional<T> right) {
        return left.isPresent() ? left : right;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Optional<T> or(Optional<T> left, Supplier<Optional<T>> right) {
        return left.isPresent() ? left : right.get();
    }

    public static boolean isEmpty(String string) {
        return !notEmpty(string);
    }

    public static boolean notEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    public static boolean arrayNotEmpty(Object array) {
        return array != null && Array.getLength(array) != 0;
    }

    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T>... streams) {
        return Arrays.stream(streams).reduce(Stream::concat).orElse(Stream.empty());
    }

    public static String[] emptyArray() {
        return EMPTY_STRING_ARRAY;
    }
}
