package io.leangen.graphql.generator.mapping;

import io.leangen.graphql.execution.ResolutionEnvironment;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Objects;

public class ArgumentInjectorParams {

    private final Object input;
    private final AnnotatedType type;
    private final Parameter parameter;
    private final ResolutionEnvironment resolutionEnvironment;

    private ArgumentInjectorParams(Object input, AnnotatedType type, Parameter parameter, ResolutionEnvironment resolutionEnvironment) {
        this.input = input;
        this.type = Objects.requireNonNull(type);
        this.parameter = parameter;
        this.resolutionEnvironment = Objects.requireNonNull(resolutionEnvironment);
    }

    public Object getInput() {
        return input;
    }

    public AnnotatedType getType() {
        return type;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public ResolutionEnvironment getResolutionEnvironment() {
        return resolutionEnvironment;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Object input;
        private AnnotatedType type;
        private Parameter parameter;
        private ResolutionEnvironment resolutionEnvironment;

        public Builder withInput(Object input) {
            this.input = input;
            return this;
        }

        public Builder withType(AnnotatedType type) {
            this.type = type;
            return this;
        }

        public Builder withParameter(Parameter parameter) {
            this.parameter = parameter;
            return this;
        }

        public Builder withResolutionEnvironment(ResolutionEnvironment resolutionEnvironment) {
            this.resolutionEnvironment = resolutionEnvironment;
            return this;
        }

        public ArgumentInjectorParams build() {
            return new ArgumentInjectorParams(input, type, parameter, resolutionEnvironment);
        }
    }
}
