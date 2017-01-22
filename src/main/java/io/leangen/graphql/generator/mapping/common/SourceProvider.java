package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.annotations.GraphQLResolverSource;
import io.leangen.graphql.query.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class SourceProvider extends InputValueDeserializer {
    
    @Override
    public Object getInputValue(Object input, AnnotatedType type, ResolutionContext resolutionContext) {
        return input == null ? resolutionContext.source : super.getInputValue(input, type, resolutionContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLResolverSource.class);
    }
}
