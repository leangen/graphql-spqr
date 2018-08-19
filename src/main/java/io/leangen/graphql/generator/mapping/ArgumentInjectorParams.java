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

    public ArgumentInjectorParams(Object input, AnnotatedType type, Parameter parameter, ResolutionEnvironment resolutionEnvironment) {
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
}
