package io.leangen.graphql.generator;

import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * Created by bojan.tomic on 7/20/16.
 */
public class MethodInvoker extends Executable {

    protected AnnotatedType enclosingType;
    protected AnnotatedType returnType;

    public MethodInvoker(Method resolverMethod, AnnotatedType enclosingType) {
        this.delegate = resolverMethod;
        this.enclosingType = enclosingType;
        this.returnType = resolveReturnType(enclosingType);
    }

    @Override
    public Object execute(Object target, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return ((Method) delegate).invoke(target, args);
    }

    @Override
    public AnnotatedType getReturnType() {
        return returnType;
    }


    public AnnotatedType resolveReturnType(AnnotatedType enclosingType) {
        if (delegate.isAnnotationPresent(GraphQLQuery.class) &&
                delegate.getAnnotation(GraphQLQuery.class).wrapper() != Void.class) {
            return GenericTypeReflector.updateAnnotations(
                    GenericTypeReflector.annotate(delegate.getAnnotation(GraphQLQuery.class).wrapper()),
                    ((Method) delegate).getAnnotatedReturnType().getAnnotations());
        }
        return ClassUtils.getReturnType(((Method) delegate), enclosingType);
    }

    @Override
    public int getParameterCount() {
        return ((Method) delegate).getParameterCount();
    }

    @Override
    public AnnotatedType[] getAnnotatedParameterTypes() {
        return ClassUtils.getParameterTypes((Method) delegate, enclosingType);
    }

    @Override
    public Parameter[] getParameters() {
        return ((Method) delegate).getParameters();
    }

    @Override
    public String getWrappedAttribute() {
        if (delegate.isAnnotationPresent(GraphQLQuery.class) &&
                delegate.getAnnotation(GraphQLQuery.class).wrapper() != Void.class &&
                !Map.class.isAssignableFrom(((Method) delegate).getReturnType())) {

            String attributeName;
            if (!delegate.getAnnotation(GraphQLQuery.class).attribute().isEmpty()) {
                attributeName = delegate.getAnnotation(GraphQLQuery.class).attribute();
            } else if (((Method) delegate).getName().startsWith("get")) {
                attributeName = ClassUtils.getFieldNameFromGetter(((Method) delegate));
            } else {
                attributeName = ((Method) delegate).getName();
            }
            //TODO move this check to builder
//			DomainType domainType = new DomainType(((Method) delegate).getAnnotation(GraphQLQuery.class).wrapper());
//			if (domainType.hasField(attributeName)) {
            return attributeName;
//			} else {
//				throw new IllegalArgumentException("Method " + ((Method) delegate).getDeclaringClass().getName() + "#" + ((Method) delegate).getName()
//						+ " wraps attribute '" + attributeName + "' which is invalid for wrapper type " + returnType.getTypeName());
//			}
        }
        return null;
    }

    @Override
    public String toString() {
        return ((Method) delegate).getDeclaringClass().getName() + "#"
                + ((Method) delegate).getName()
                + " -> " + returnType.toString();
    }
}
