package io.leangen.graphql.generator.mapping.common;

import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ContextInjector extends InputValueDeserializer {
    
    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        return params.getInput() == null ? params.getResolutionEnvironment().context : super.getArgumentValue(params);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLContext.class);
    }
}
