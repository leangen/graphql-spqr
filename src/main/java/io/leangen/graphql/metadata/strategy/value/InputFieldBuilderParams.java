package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.execution.GlobalEnvironment;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class InputFieldBuilderParams {

    private final AnnotatedType type;
    private final GlobalEnvironment environment;
    private final List<Class<?>> concreteSubTypes;

    /**
     * @param type Java type (used as query input) to be analyzed for deserializable fields
     * @param environment The global environment
     * @param concreteSubTypes Concrete subtypes of {@code type} (if it is abstract and abstract input handling is enabled)
     */
    private InputFieldBuilderParams(AnnotatedType type, GlobalEnvironment environment, List<Class<?>> concreteSubTypes) {
        this.type = Objects.requireNonNull(type);
        this.environment = Objects.requireNonNull(environment);
        this.concreteSubTypes = concreteSubTypes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public AnnotatedType getType() {
        return type;
    }

    public GlobalEnvironment getEnvironment() {
        return environment;
    }

    public List<Class<?>> getConcreteSubTypes() {
        return concreteSubTypes;
    }

    public static class Builder {
        private AnnotatedType type;
        private GlobalEnvironment environment;
        private List<Class<?>> concreteSubTypes = Collections.emptyList();

        public Builder withType(AnnotatedType type) {
            this.type = type;
            return this;
        }

        public Builder withEnvironment(GlobalEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public Builder withConcreteSubTypes(List<Class<?>> concreteSubTypes) {
            this.concreteSubTypes = concreteSubTypes;
            return this;
        }

        public InputFieldBuilderParams build() {
            return new InputFieldBuilderParams(type, environment, concreteSubTypes);
        }
    }
}
