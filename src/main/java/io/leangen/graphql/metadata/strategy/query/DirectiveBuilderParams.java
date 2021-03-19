package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;

import java.util.Collections;
import java.util.List;

public class DirectiveBuilderParams {

    private final GlobalEnvironment environment;
    private final InputFieldBuilder inputFieldBuilders;
    private final List<Class<?>> concreteSubTypes;

    private DirectiveBuilderParams(GlobalEnvironment environment, InputFieldBuilder inputFieldBuilders, List<Class<?>> concreteSubTypes) {
        this.environment = environment;
        this.inputFieldBuilders = inputFieldBuilders;
        this.concreteSubTypes = concreteSubTypes;
    }

    public GlobalEnvironment getEnvironment() {
        return environment;
    }

    public InputFieldBuilder getInputFieldBuilders() {
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
        private InputFieldBuilder inputFieldBuilders;
        private List<Class<?>> concreteSubTypes = Collections.emptyList();

        public Builder withEnvironment(GlobalEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public Builder withInputFieldBuilder(InputFieldBuilder inputFieldBuilders) {
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
