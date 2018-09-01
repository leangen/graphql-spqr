package io.leangen.graphql.metadata.execution;

import io.leangen.graphql.metadata.exceptions.MappingException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

public abstract class Executable<T extends AnnotatedElement & Member> {

    T delegate;
    MethodHandle handle;

    abstract public Object execute(Object target, Object[] args) throws Throwable;

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

    public T getDelegate() {
        return delegate;
    }

    MethodHandle prepareHandle(MethodHandleProvider provider) {
        try {
            MethodHandle handle = provider.get();
            if (Modifier.isStatic(delegate.getModifiers())) {
                handle = MethodHandles.dropArguments(handle, 0, Object.class);
            }
            return handle.asFixedArity();
        } catch (IllegalAccessException e) {
            throw new MappingException("Method '" + delegate.getName() + "' can not be mapped", e);
        }
    }

    interface MethodHandleProvider {
        MethodHandle get() throws IllegalAccessException;
    }

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

    @Override
    public String toString() {
        return delegate.toString();
    }
}
