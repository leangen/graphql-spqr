package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;

public class ArgumentBuilderParams {

    private final Method resolverMethod;
    private final AnnotatedType declaringType;
    private final InclusionStrategy inclusionStrategy;
    private final TypeTransformer typeTransformer;
    private final MessageBundle messageBundle;

    ArgumentBuilderParams(Method resolverMethod, AnnotatedType declaringType, InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer, MessageBundle messageBundle) {
        this.resolverMethod = resolverMethod;
        this.declaringType = declaringType;
        this.inclusionStrategy = inclusionStrategy;
        this.typeTransformer = typeTransformer;
        this.messageBundle = messageBundle;
    }

    public Method getResolverMethod() {
        return resolverMethod;
    }

    public AnnotatedType getDeclaringType() {
        return declaringType;
    }

    public InclusionStrategy getInclusionStrategy() {
        return inclusionStrategy;
    }

    public TypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    public MessageBundle getMessageBundle() {
        return messageBundle;
    }
}
