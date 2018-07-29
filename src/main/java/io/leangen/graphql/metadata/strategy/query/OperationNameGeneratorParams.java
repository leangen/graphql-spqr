package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.messages.MessageBundle;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;

public class OperationNameGeneratorParams<T extends Member & AnnotatedElement> {

    private final T element;
    private final AnnotatedType declaringType;
    private final Object instance;
    private final MessageBundle messageBundle;

    OperationNameGeneratorParams(T element, AnnotatedType declaringType, Object instance, MessageBundle messageBundle) {
        this.element = element;
        this.declaringType = declaringType;
        this.instance = instance;
        this.messageBundle = messageBundle;
    }

    public T getElement() {
        return element;
    }

    public AnnotatedType getDeclaringType() {
        return declaringType;
    }

    public Object getInstance() {
        return instance;
    }

    public MessageBundle getMessageBundle() {
        return messageBundle;
    }

    public boolean isField() {
        return element instanceof Field;
    }
}
