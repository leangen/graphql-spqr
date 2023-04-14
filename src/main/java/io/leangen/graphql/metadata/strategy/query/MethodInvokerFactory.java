package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.execution.Executable;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public interface MethodInvokerFactory {

    Executable<Method> create(Supplier<Object> targetSupplier, Method resolverMethod, AnnotatedType enclosingType, Class<?> exposedType);
}
