package io.leangen.graphql.metadata.execution;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Created by soumik.dutta on 9/21/24.
 */
public class FixedLambdaInvoker extends LambdaInvoker {
    final private Supplier<Object> targetSupplier;

    public FixedLambdaInvoker(final Supplier<Object> targetSupplier, final Method resolverMethod, final AnnotatedType enclosingType, final boolean useSetAccessible) throws Exception {
        super(resolverMethod, enclosingType, useSetAccessible);
        this.targetSupplier = targetSupplier;
    }

    @Override
    public Object execute(final Object target, final Object[] arguments) {
        return this.lambdaGetter.apply(this.targetSupplier.get());
    }
}