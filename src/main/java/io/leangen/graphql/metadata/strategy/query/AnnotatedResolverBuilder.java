package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.value.Property;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A resolver builder that exposes only the methods explicitly annotated with {@link GraphQLQuery}
 */
public class AnnotatedResolverBuilder extends PublicResolverBuilder {

    public AnnotatedResolverBuilder() {
        this.operationInfoGenerator = new DefaultOperationInfoGenerator();
        this.argumentBuilder = new AnnotatedArgumentBuilder();
        this.propertyElementReducer = AnnotatedResolverBuilder::annotatedElementReducer;
        withDefaultFilters();
    }

    @Override
    protected boolean isQuery(Method method, ResolverBuilderParams params) {
        return method.isAnnotationPresent(GraphQLQuery.class);
    }

    @Override
    protected boolean isQuery(Field field, ResolverBuilderParams params) {
        return field.isAnnotationPresent(GraphQLQuery.class);
    }

    @Override
    protected boolean isQuery(Property property, ResolverBuilderParams params) {
        return isQuery(property.getGetter(), params) || isQuery(property.getField(), params);
    }

    @Override
    protected boolean isMutation(Method method, ResolverBuilderParams params) {
        return method.isAnnotationPresent(GraphQLMutation.class);
    }

    @Override
    protected boolean isSubscription(Method method, ResolverBuilderParams params) {
        return method.isAnnotationPresent(GraphQLSubscription.class);
    }

    private static TypedElement annotatedElementReducer(TypedElement field, TypedElement getter) {
        if (field.isAnnotationPresent(GraphQLQuery.class) && getter.isAnnotationPresent(GraphQLQuery.class)) {
            throw new TypeMappingException("Ambiguous mapping of " + field);
        }
        return field.isAnnotationPresent(GraphQLQuery.class) ? field : getter;
    }
}
