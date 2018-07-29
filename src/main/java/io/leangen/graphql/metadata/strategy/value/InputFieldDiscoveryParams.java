package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;

import java.lang.reflect.AnnotatedType;

public class InputFieldDiscoveryParams {

    private final AnnotatedType type;
    private final InclusionStrategy inclusionStrategy;
    private final TypeTransformer typeTransformer;
    private final MessageBundle messageBundle;

    /**
     * @param type Java type (used as query input) to be analyzed for deserializable fields
     * @param inclusionStrategy The strategy that decides which input fields are acceptable
     * @param typeTransformer Transformer used to pre-process the types (can be used to complete the missing generics etc)
     * @param messageBundle The global translation message bundle
     */
    public InputFieldDiscoveryParams(AnnotatedType type, InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer, MessageBundle messageBundle) {
        this.type = type;
        this.inclusionStrategy = inclusionStrategy;
        this.typeTransformer = typeTransformer;
        this.messageBundle = messageBundle;
    }

    public AnnotatedType getType() {
        return type;
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
