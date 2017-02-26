package io.leangen.graphql.metadata.execution;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;

public abstract class Executable {

    protected AnnotatedElement delegate;

    abstract public Object execute(Object target, Object[] args) throws InvocationTargetException, IllegalAccessException;

    abstract public AnnotatedType getReturnType();

    abstract public int getParameterCount();

    abstract public AnnotatedType[] getAnnotatedParameterTypes();

    abstract public Parameter[] getParameters();

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Executable && ((Executable) other).delegate.equals(this.delegate);
    }
}
