package io.leangen.graphql.metadata;

import io.leangen.graphql.util.Utils;

public class TypeDiscriminatorField {

    private final String name;
    private final String description;
    private final String[] values;

    public TypeDiscriminatorField(String name, String description, String... values) {
        this.name = Utils.requireNonEmpty(name);
        this.description = description;
        this.values = Utils.requireNonEmpty(values);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String[] getValues() {
        return values;
    }
}
