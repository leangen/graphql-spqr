package io.leangen.graphql.generator.mapping;

import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.metadata.OperationArgument;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Objects;

public class ArgumentInjectorParams {

    private final Object input;
    private final boolean present;
    private final OperationArgument argument;
    private final ResolutionEnvironment resolutionEnvironment;

    public ArgumentInjectorParams(Object input, boolean present, OperationArgument argument, ResolutionEnvironment resolutionEnvironment) {
        this.input = input;
        this.present = present;
        this.argument = argument;
        this.resolutionEnvironment = Objects.requireNonNull(resolutionEnvironment);
    }

    public Object getInput() {
        return input;
    }

    public boolean isPresent() {
        return present;
    }

    public AnnotatedType getType() {
        return argument.getJavaType();
    }

    public AnnotatedType getBaseType() {
        return argument.getBaseType();
    }

    public Parameter getParameter() {
        return argument.getParameter();
    }

    public OperationArgument getArgument() {
        return argument;
    }

    public ResolutionEnvironment getResolutionEnvironment() {
        return resolutionEnvironment;
    }
}
