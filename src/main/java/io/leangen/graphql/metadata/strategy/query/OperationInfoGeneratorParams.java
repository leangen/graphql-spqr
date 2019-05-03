package io.leangen.graphql.metadata.strategy.query;

import graphql.language.OperationDefinition;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.messages.EmptyMessageBundle;
import io.leangen.graphql.metadata.messages.MessageBundle;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;
import java.util.function.Supplier;

public class OperationInfoGeneratorParams {

    private final TypedElement element;
    private final AnnotatedType declaringType;
    private final Supplier<Object> instanceSupplier;
    private final MessageBundle messageBundle;
    private final OperationDefinition.Operation operationType;

    OperationInfoGeneratorParams(TypedElement element, AnnotatedType declaringType, Supplier<Object> instanceSupplier,
                                 MessageBundle messageBundle, OperationDefinition.Operation operationType) {
        this.element = Objects.requireNonNull(element);
        this.declaringType = Objects.requireNonNull(declaringType);
        this.instanceSupplier = instanceSupplier;
        this.messageBundle = messageBundle != null ? messageBundle : EmptyMessageBundle.INSTANCE;
        this.operationType = operationType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TypedElement getElement() {
        return element;
    }

    public AnnotatedType getDeclaringType() {
        return declaringType;
    }

    public Supplier<Object> getInstanceSupplier() {
        return instanceSupplier;
    }

    public MessageBundle getMessageBundle() {
        return messageBundle;
    }

    public OperationDefinition.Operation getOperationType() {
        return operationType;
    }

    public static class Builder {
        private TypedElement element;
        private AnnotatedType declaringType;
        private Supplier<Object> instanceSupplier;
        private MessageBundle messageBundle;
        private OperationDefinition.Operation operationType;

        public Builder withElement(TypedElement element) {
            this.element = element;
            return this;
        }

        public Builder withDeclaringType(AnnotatedType declaringType) {
            this.declaringType = declaringType;
            return this;
        }

        public Builder withInstance(Supplier<Object> instanceSupplier) {
            this.instanceSupplier = instanceSupplier;
            return this;
        }

        public Builder withMessageBundle(MessageBundle messageBundle) {
            this.messageBundle = messageBundle;
            return this;
        }

        public Builder withOperationType(OperationDefinition.Operation operationType) {
            this.operationType = operationType;
            return this;
        }

        public OperationInfoGeneratorParams build() {
            return new OperationInfoGeneratorParams(element, declaringType, instanceSupplier, messageBundle, operationType);
        }
    }
}
