package io.leangen.graphql.metadata.execution;

import io.leangen.graphql.util.ClassUtils;

import java.lang.invoke.*;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by soumik.dutta on 9/21/24.
 */
public class LambdaInvoker extends Executable<Method> {
    private static final Parameter[] NO_PARAMETERS = {};
    private static final AnnotatedType[] NO_ANNOTATED_TYPES = {};
    final Function<Object, Object> lambdaGetter;
    private final AnnotatedType returnType;


    public LambdaInvoker(final Method resolverMethod, final AnnotatedType enclosingType, final boolean useSetAccessible) throws Exception {
        this.delegate = resolverMethod;
        this.returnType = resolveReturnType(enclosingType);
        final Optional<Function<Object, Object>> getter = createGetter(resolverMethod, useSetAccessible);
        if (getter.isEmpty()) {
            throw new Exception("Cannot create a lambda getter for " + resolverMethod.getName());
        }

        this.lambdaGetter = getter.get();
    }

    public static Optional<Function<Object, Object>> createGetter(final Method method, final boolean useSetAccessible) throws Exception {
        if (method != null) {
            if (method.getParameterCount() > 0) {
                throw new Exception(method.getName() + " requires more than one argument");
            }

            try {
                method.setAccessible(useSetAccessible);
                MethodHandles.Lookup lookupMe = MethodHandles.lookup();
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), lookupMe);
                MethodHandle virtualMethodHandle = lookup.unreflect(method);

                CallSite site = LambdaMetafactory.metafactory(lookup, "apply", MethodType.methodType(Function.class),
                        MethodType.methodType(Object.class, Object.class), virtualMethodHandle,
                        MethodType.methodType(method.getReturnType(), method.getDeclaringClass()));

                @SuppressWarnings("unchecked") Function<Object, Object> getterFunction = (Function<Object, Object>) site.getTarget()
                        .invokeExact();
                return Optional.of(getterFunction);
            } catch (Throwable e) {
                //
                // if we cant make a dynamic lambda here, then we give up and let the old property fetching code do its thing
                // this can happen on runtimes such as GraalVM native where LambdaMetafactory is not supported
                // and will throw something like :
                //
                //    com.oracle.svm.core.jdk.UnsupportedFeatureError: Defining hidden classes at runtime is not supported.
                //        at org.graalvm.nativeimage.builder/com.oracle.svm.core.util.VMError.unsupportedFeature(VMError.java:89)
            }
        }
        return Optional.empty();
    }

    @Override
    public Object execute(final Object target, final Object[] args) {
        return lambdaGetter.apply(target);
    }

    @Override
    final public AnnotatedType getReturnType() {
        return returnType;
    }

    private AnnotatedType resolveReturnType(final AnnotatedType enclosingType) {
        return ClassUtils.getReturnType(delegate, enclosingType);
    }


    @Override
    final public int getParameterCount() {
        return 0;
    }

    @Override
    final public AnnotatedType[] getAnnotatedParameterTypes() {
        return NO_ANNOTATED_TYPES;
    }

    @Override
    final public Parameter[] getParameters() {
        return NO_PARAMETERS;
    }
}