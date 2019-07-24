package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.InputFieldBuilderRegistry;

import java.util.Collections;
import java.util.List;

public class DirectiveBuilderParams {

    private final GlobalEnvironment environment;
    private final InputFieldBuilderRegistry inputFieldBuilders;
    private final List<Class<?>> concreteSubTypes;

    private DirectiveBuilderParams(GlobalEnvironment environment, InputFieldBuilderRegistry inputFieldBuilders, List<Class<?>> concreteSubTypes) {
        this.environment = environment;
        this.inputFieldBuilders = inputFieldBuilders;
        this.concreteSubTypes = concreteSubTypes;
    }

    public GlobalEnvironment getEnvironment() {
        return environment;
    }

    public InputFieldBuilderRegistry getInputFieldBuilders() {
        return inputFieldBuilders;
    }

    public List<Class<?>> getConcreteSubTypes() {
        return concreteSubTypes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GlobalEnvironment environment;
        private InputFieldBuilderRegistry inputFieldBuilders;
        private List<Class<?>> concreteSubTypes = Collections.emptyList();

        public Builder withEnvironment(GlobalEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public Builder withInputFieldBuilders(InputFieldBuilderRegistry inputFieldBuilders) {
            this.inputFieldBuilders = inputFieldBuilders;
            return this;
        }

        public Builder withConcreteSubTypes(List<Class<?>> concreteSubTypes) {
            this.concreteSubTypes = concreteSubTypes;
            return this;
        }

        public DirectiveBuilderParams build() {
            return new DirectiveBuilderParams(environment, inputFieldBuilders, concreteSubTypes);
        }
    }
}
