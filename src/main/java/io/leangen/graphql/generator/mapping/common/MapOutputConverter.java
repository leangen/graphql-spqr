package io.leangen.graphql.generator.mapping.common;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Only used to trigger the conversion of map keys and values
 */
public class MapOutputConverter implements DelegatingOutputConverter<Map<?, ?>, Map<?, ?>> {

    @Override
    public Map<?, ?> convertOutput(Map<?, ?> original, AnnotatedType type, ResolutionEnvironment env) {
        return processMap(original, env.getDerived(type, 0), env.getDerived(type, 1), env);
    }

    @Override
    public List<AnnotatedType> getDerivedTypes(AnnotatedType mapType) {
        return Arrays.asList(getElementType(mapType, 0), getElementType(mapType, 1));
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return ClassUtils.isSuperClass(Map.class, type);
    }

    private Map<?, ?> processMap(Map<?, ?> map, AnnotatedType keyType, AnnotatedType valueType, ResolutionEnvironment env) {
        Map<?, ?> processed = new LinkedHashMap<>();
        map.forEach((k, v) -> processed.put(
                env.convertOutput(k, env.resolver.getTypedElement(), keyType),
                env.convertOutput(v, env.resolver.getTypedElement(), valueType)));
        return processed;
    }

    private AnnotatedType getElementType(AnnotatedType mapType, int typeParameterIndex) {
        return GenericTypeReflector.getTypeParameter(mapType, Map.class.getTypeParameters()[typeParameterIndex]);
    }
}
