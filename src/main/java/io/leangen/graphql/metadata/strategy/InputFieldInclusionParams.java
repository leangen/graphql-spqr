package io.leangen.graphql.metadata.strategy;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class InputFieldInclusionParams {

    private final AnnotatedType declaringType;
    private final Class<?> elementDeclaringClass;
    private final List<AnnotatedElement> elements;
    private final boolean directlyDeserializable;
    private final boolean deserializableInSubType;

    private InputFieldInclusionParams(AnnotatedType declaringType, Class<?> elementDeclaringClass, List<AnnotatedElement> elements,
                                      boolean directlyDeserializable, boolean deserializableInSubType) {
        this.declaringType = declaringType;
        this.elementDeclaringClass = elementDeclaringClass;
        this.elements = elements;
        this.directlyDeserializable = directlyDeserializable;
        this.deserializableInSubType = deserializableInSubType;
    }

    public AnnotatedType getDeclaringType() {
        return declaringType;
    }

    public Class<?> getElementDeclaringClass() {
        return elementDeclaringClass;
    }

    public List<AnnotatedElement> getElements() {
        return elements;
    }

    public boolean isDirectlyDeserializable() {
        return directlyDeserializable;
    }

    public boolean isDeserializableInSubType() {
        return deserializableInSubType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AnnotatedType type;
        private Class<?> declaringClass;
        private List<AnnotatedElement> elements;
        private boolean directlyDeserializable;
        private boolean deserializableInSubType;

        public Builder withType(AnnotatedType type) {
            this.type = type;
            return this;
        }

        public Builder withElementDeclaringClass(Class<?> declaringClass) {
            this.declaringClass = declaringClass;
            return this;
        }

        public Builder withElements(List<AnnotatedElement> elements) {
            this.elements = elements;
            return this;
        }

        public Builder withDeserializationInfo(boolean directlyDeserializable, boolean deserializableInSubType) {
            this.directlyDeserializable = directlyDeserializable;
            this.deserializableInSubType = deserializableInSubType;
            return this;
        }

        public InputFieldInclusionParams build() {
            return new InputFieldInclusionParams(type, declaringClass, elements, directlyDeserializable, deserializableInSubType);
        }
    }
}
