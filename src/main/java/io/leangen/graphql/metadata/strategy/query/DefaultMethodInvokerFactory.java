package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class DefaultMethodInvokerFactory implements MethodInvokerFactory {
    private static final Logger log = LoggerFactory.getLogger(DefaultMethodInvokerFactory.class);
    private final AtomicBoolean USE_SET_ACCESSIBLE = new AtomicBoolean(true);
    private final AtomicBoolean USE_LAMBDA_FACTORY = new AtomicBoolean(true);

    @Override
    public Executable<Method> create(Supplier<Object> targetSupplier, Method resolverMethod, AnnotatedType enclosingType, Class<?> exposedType) {
        try {
            if (USE_LAMBDA_FACTORY.get()) {
                return targetSupplier == null ? new LambdaInvoker(resolverMethod, enclosingType,
                        USE_SET_ACCESSIBLE.get()) : new FixedLambdaInvoker(targetSupplier, resolverMethod, enclosingType,
                        USE_SET_ACCESSIBLE.get());
            }
        } catch (Exception e) {
            log.warn("Lambda Invokers could not be used for {} because {}", resolverMethod, e.toString());
        }

        return targetSupplier == null ? new MethodInvoker(resolverMethod, enclosingType) : new FixedMethodInvoker(targetSupplier,
                resolverMethod, enclosingType);
    }
}
