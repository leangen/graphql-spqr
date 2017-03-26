package io.leangen.graphql.metadata.execution;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;

public abstract class Executable {

    AnnotatedElement delegate;

    abstract public Object execute(Object target, Object[] args) throws InvocationTargetException, IllegalAccessException;

    abstract public AnnotatedType getReturnType();

    /**
     * Returns the number of formal parameters this executable takes.
     * May differ from {@code getParameters().length}.
     *
     * @return The number of formal parameter this executable takes
     * */
    abstract public int getParameterCount();

    abstract public AnnotatedType[] getAnnotatedParameterTypes();

    abstract public Parameter[] getParameters();

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    /**
     * Two {@code Executable}s are considered equal either if their wrapped fields/methods are equal
     * or if one wraps a field and the other its corresponding getter/setter.
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Executable && ((Executable) other).delegate.equals(this.delegate);
    }
}
