package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.annotations.types.GraphQLType;

@GraphQLType(name = "JsonPatch")
public class JsonPatch {
    private JsonPatchOp op;

    private String path;

    private String from;

    private Object value;

    public JsonPatchOp getOp() {
        return op;
    }

    public void setOp(JsonPatchOp op) {
        this.op = op;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @GraphQLScalar
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public enum JsonPatchOp {
        add,
        remove,
        replace,
        copy,
        move,
        test
    }
}
