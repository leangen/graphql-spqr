package io.leangen.graphql.util;

import java.util.Optional;

/**
 * A collection of utility methods
 */
public class Utils {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Optional<T> or(Optional<T> left, Optional<T> right) {
        return left.isPresent() ? left : right;
    }

    public static boolean notEmpty(String string) {
        return string != null && !string.isEmpty();
    }
}
