package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.generator.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by bojan.tomic on 3/21/17.
 */
@SuppressWarnings("WeakerAccess")
public abstract class FilteredResolverBuilder implements ResolverBuilder {

    protected OperationNameGenerator operationNameGenerator;
    protected ResolverArgumentBuilder argumentBuilder;
    protected TypeTransformer transformer;
    protected List<Predicate<Member>> filters = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public FilteredResolverBuilder withOperationNameGenerator(OperationNameGenerator operationNameGenerator) {
        this.operationNameGenerator = operationNameGenerator;
        return this;
    }

    @SuppressWarnings("unchecked")
    public FilteredResolverBuilder withResolverArgumentBuilder(ResolverArgumentBuilder argumentBuilder) {
        this.argumentBuilder = argumentBuilder;
        return this;
    }

    @SuppressWarnings("unchecked")
    public FilteredResolverBuilder withTypeTransformer(TypeTransformer transformer) {
        this.transformer = transformer;
        return this;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final FilteredResolverBuilder withFilters(Predicate<Member>... filters) {
        Collections.addAll(this.filters, filters);
        return this;
    }

    public FilteredResolverBuilder withDefaultFilters() {
        return withFilters(REAL_ONLY, NOT_IGNORED);
    }

    protected List<Predicate<Member>> getFilters() {
        return filters.isEmpty() ? Collections.singletonList(ACCEPT_ALL) : filters;
    }

    protected AnnotatedType getFieldType(Field field, AnnotatedType declaringType) {
        try {
            return transformer.transform(ClassUtils.getFieldType(field, declaringType));
        } catch (TypeMappingException e) {
            throw new TypeMappingException(field, declaringType, e);
        }
    }

    protected AnnotatedType getReturnType(Method method, AnnotatedType declaringType) {
        try {
            return transformer.transform(ClassUtils.getReturnType(method, declaringType));
        } catch (TypeMappingException e) {
            throw new TypeMappingException(method, declaringType, e);
        }
    }
}
