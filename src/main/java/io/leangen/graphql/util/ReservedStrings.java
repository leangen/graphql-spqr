package io.leangen.graphql.util;

import io.leangen.graphql.metadata.DefaultValue;

public class ReservedStrings {

    public static final String NULL = "\u200B\u202F\uFEFF<null>\u200B\u202F\uFEFF";
    public static final String NONE = "\u200B\u202F\uFEFF<none>\u200B\u202F\uFEFF";

    public static String decode(String value) {
        return NULL.equals(value) ? null : value;
    }

    public static DefaultValue decodeDefault(String value) {
        if (NONE.equals(value)) return DefaultValue.EMPTY;
        return NULL.equals(value) ? DefaultValue.NULL : new DefaultValue(value);
    }
}
