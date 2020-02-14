package io.leangen.graphql.generator.mapping.common;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Only used to trigger the conversion of collection elements
 */
public class CollectionOutputConverter implements DelegatingOutputConverter<Collection<?>, Collection<?>> {

    @Override
    public Collection<?> convertOutput(Collection<?> original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return processCollection(original, resolutionEnvironment.getDerived(type, 0), resolutionEnvironment);
    }

    @Override
    public List<AnnotatedType> getDerivedTypes(AnnotatedType collectionType) {
        return Collections.singletonList(getElementType(collectionType));
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return ClassUtils.isSuperClass(Collection.class, type);
    }

    private List<?> processCollection(Collection<?> collection, AnnotatedType elementType, ResolutionEnvironment env) {
        return collection.stream()
                .map(e -> env.convertOutput(e, env.resolver.getTypedElement(), elementType))
                .collect(Collectors.toList());
    }

    private AnnotatedType getElementType(AnnotatedType collectionType) {
        return GenericTypeReflector.getTypeParameter(collectionType, Collection.class.getTypeParameters()[0]);
    }
}
