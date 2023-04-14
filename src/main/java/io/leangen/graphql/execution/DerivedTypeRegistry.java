package io.leangen.graphql.execution;

import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.metadata.TypedElement;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
class DerivedTypeRegistry {

    private final Map<AnnotatedType, List<AnnotatedType>> derivedTypes;

    DerivedTypeRegistry(List<TypedElement> elements, List<DelegatingOutputConverter> derivers) {
        this.derivedTypes = new IdentityHashMap<>();
        elements.forEach(element -> derive(element, element.getJavaType(), derivers));
    }

    private void registerDerivatives(AnnotatedElement element, AnnotatedType type, List<AnnotatedType> derivedTypes, List<DelegatingOutputConverter> derivers) {
        derivedTypes.forEach(derived -> {
            this.derivedTypes.computeIfAbsent(type, k -> new ArrayList<>());
            this.derivedTypes.get(type).add(derived);
            derive(element, derived, derivers);
        });
    }

    @SuppressWarnings("unchecked")
    private void derive(AnnotatedElement element, AnnotatedType type, List<DelegatingOutputConverter> derivers) {
        derivers.stream()
                .filter(deriver -> deriver.supports(element, type))
                .findFirst()
                .ifPresent(deriver -> registerDerivatives(element, type, deriver.getDerivedTypes(type), derivers));
    }

    List<AnnotatedType> getDerived(AnnotatedType type) {
        return derivedTypes.getOrDefault(type, Collections.emptyList());
    }
}
