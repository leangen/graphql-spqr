package io.leangen.graphql.util;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A collection of utility methods
 */
@SuppressWarnings("WeakerAccess")
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

    @SafeVarargs
    public static <T> Stream<T> flatten(Optional<? extends T>... optionals) {
        return Arrays.stream(optionals).filter(Optional::isPresent).map(Optional::get);
    }

    public static boolean isEmpty(String string) {
        return !isNotEmpty(string);
    }

    public static boolean isNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    public static String coalesce(String... values) {
        return Arrays.stream(values)
                .filter(Utils::isNotEmpty)
                .findFirst()
                .orElse(null);
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

    public static <T> boolean isEmpty(T[] array) {
        return !isNotEmpty(array);
    }

    public static <T>  boolean isNotEmpty(T[] array) {
        return array != null && array.length > 0;
    }

    public static <T> T[] requireNonEmpty(T[] array) {
        if (isEmpty(array)) {
            throw new IllegalArgumentException("Empty array is not a valid value");
        }
        return array;
    }

    public static int indexOf(String[] strings, String element, int missingIndex) {
        requireNonEmpty(element);
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].equals(element)) {
                return i;
            }
        }
        return missingIndex;
    }

    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T>... streams) {
        return Arrays.stream(streams).reduce(Stream::concat).orElse(Stream.empty());
    }

    public static <C extends Collection<T>, T> boolean isEmpty(C collection) {
        return collection == null || collection.isEmpty();
    }

    public static <C extends Collection<T>, T> boolean isNotEmpty(C collection) {
        return !isEmpty(collection);
    }

    public static <C extends Collection<T>, T> C defaultIfEmpty(C collection, C fallback) {
        return collection == null || collection.isEmpty() ? fallback : collection;
    }

    public static <T> Stream<T> stream(Collection<T> collection) {
        return collection == null ? Stream.empty() : collection.stream();
    }

    public static <T> Stream<T> extractInstances(Collection<? super T> collection, Class<T> clazz) {
        return collection.stream().filter(clazz::isInstance).map(clazz::cast);
    }

    public static String[] emptyArray() {
        return EMPTY_STRING_ARRAY;
    }

    public static String[] emptyIfNull(String[] array) {
        return array == null ? emptyArray() : array;
    }

    public static String requireNonEmpty(String value) {
        if (isEmpty(value)) {
            throw new IllegalArgumentException("Empty string is not a valid value");
        }
        return value;
    }

    public static <T> List<T> singletonList(T element) {
        return element == null ? Collections.emptyList() : Collections.singletonList(element);
    }

    public static <T> List<T> asList(T[] elements) {
        return elements == null || elements.length == 0 ? Collections.emptyList() : Arrays.asList(elements);
    }

    public static <T> Predicate<T> acceptAll() {
        return x -> true;
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> failed = new CompletableFuture<>();
        failed.completeExceptionally(throwable);
        return failed;
    }

    public static <K, V> Map<K, V> put(Map<K, V> map, K k, V v) {
        Map<K, V> fresh = new HashMap<>(map);
        fresh.put(k, v);
        return fresh;
    }
}
