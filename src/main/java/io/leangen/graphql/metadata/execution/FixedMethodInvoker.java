package io.leangen.graphql.metadata.execution;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Created by bojan.tomic on 3/5/16.
 */
public class FixedMethodInvoker extends MethodInvoker {

    private Supplier<Object> targetSupplier;

    public FixedMethodInvoker(Supplier<Object> targetSupplier, Method resolverMethod, AnnotatedType enclosingType) {
        super(resolverMethod, enclosingType);
        this.targetSupplier = targetSupplier;
    }

    @Override
    public Object execute(Object target, Object[] arguments) throws InvocationTargetException, IllegalAccessException {
        return delegate.invoke(this.targetSupplier.get(), arguments);
    }
}
