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

    public static final String NULL = "\n\t\t\n\t\t\n\ue000\ue001\ue002\ue000\n\t\t\t\t\n";

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
        return !isNotEmpty(string);
    }

    public static boolean isNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    public static String decodeNullable(String string) {
        return NULL.equals(string) ? null : string;
    }

    public static String capitalize(final String str) {
        final char firstChar = str.charAt(0);
        final char newChar = Character.toUpperCase(firstChar);
        if (firstChar == newChar) {
            // already capitalized
            return str;
        }

        char[] newChars = new char[str.length()];
        newChars[0] = newChar;
        str.getChars(1, str.length(), newChars, 1);
        return String.valueOf(newChars);
    }

    public static boolean isArrayEmpty(Object array) {
        return !isArrayNotEmpty(array);
    }

    public static boolean isArrayNotEmpty(Object array) {
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
