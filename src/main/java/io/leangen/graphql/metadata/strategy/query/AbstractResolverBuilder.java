package io.leangen.graphql.metadata.strategy.query;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

/**
 * The base class for all built-in {@code ResolverBuilder}s
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractResolverBuilder implements ResolverBuilder {

    protected OperationInfoGenerator operationInfoGenerator;
    protected ResolverArgumentBuilder argumentBuilder;
    protected BinaryOperator<TypedElement> propertyElementReducer;
    protected List<Predicate<Member>> filters = new ArrayList<>();

    public AbstractResolverBuilder withOperationInfoGenerator(OperationInfoGenerator operationInfoGenerator) {
        this.operationInfoGenerator = operationInfoGenerator;
        return this;
    }

    public AbstractResolverBuilder withResolverArgumentBuilder(ResolverArgumentBuilder argumentBuilder) {
        this.argumentBuilder = argumentBuilder;
        return this;
    }

    public AbstractResolverBuilder withPropertyElementReducer(BinaryOperator<TypedElement> propertyElementReducer) {
        this.propertyElementReducer = propertyElementReducer;
        return this;
    }

    @SafeVarargs
    public final AbstractResolverBuilder withFilters(Predicate<Member>... filters) {
        Collections.addAll(this.filters, filters);
        return this;
    }

    public AbstractResolverBuilder withDefaultFilters() {
        return withFilters(REAL_ONLY);
    }

    public static TypedElement mergePropertyElements(TypedElement field, TypedElement getter) {
        return new TypedElement(
                GenericTypeReflector.mergeAnnotations(field.getJavaType(), getter.getJavaType()),
                field.getElement(), getter.getElement());
    }

    protected List<Predicate<Member>> getFilters() {
        return filters.isEmpty() ? Collections.singletonList(ACCEPT_ALL) : filters;
    }

    protected AnnotatedType getFieldType(Field field, ResolverBuilderParams params) {
        try {
            return params.getTypeTransformer().transform(ClassUtils.getFieldType(field, params.getBeanType()));
        } catch (TypeMappingException e) {
            throw new TypeMappingException(field, params.getBeanType(), e);
        }
    }

    protected AnnotatedType getReturnType(Method method, ResolverBuilderParams params) {
        try {
            return params.getTypeTransformer().transform(ClassUtils.getReturnType(method, params.getBeanType()));
        } catch (TypeMappingException e) {
            throw new TypeMappingException(method, params.getBeanType(), e);
        }
    }
}
