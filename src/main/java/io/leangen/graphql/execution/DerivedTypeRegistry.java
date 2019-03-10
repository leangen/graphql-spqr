package io.leangen.graphql.execution;

import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

class DerivedTypeRegistry {

    private final Map<AnnotatedType, List<AnnotatedType>> derivedTypes;

    DerivedTypeRegistry(List<AnnotatedType> types, List<DelegatingOutputConverter> derivers) {
        this.derivedTypes = new IdentityHashMap<>();
        types.forEach(type -> derive(type, derivers));
    }

    private void registerDerivatives(AnnotatedType type, List<AnnotatedType> derivedTypes, List<DelegatingOutputConverter> derivers) {
        derivedTypes.forEach(derived -> {
            this.derivedTypes.computeIfAbsent(type, k -> new ArrayList<>());
            this.derivedTypes.get(type).add(derived);
            derive(derived, derivers);
        });
    }

    @SuppressWarnings("unchecked")
    private void derive(AnnotatedType type, List<DelegatingOutputConverter> derivers) {
        derivers.stream()
                .filter(deriver -> deriver.supports(type))
                .findFirst()
                .ifPresent(deriver -> registerDerivatives(type, deriver.getDerivedTypes(type), derivers));
    }

    List<AnnotatedType> getDerived(AnnotatedType type) {
        return derivedTypes.getOrDefault(type, Collections.emptyList());
    }
}
