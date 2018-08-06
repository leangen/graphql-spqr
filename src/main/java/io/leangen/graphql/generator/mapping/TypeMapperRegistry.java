package io.leangen.graphql.generator.mapping;

import io.leangen.graphql.metadata.exceptions.MappingException;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class TypeMapperRegistry {

    private final List<TypeMapper> typeMappers;

    public TypeMapperRegistry(List<TypeMapper> typeMappers) {
        this.typeMappers = Collections.unmodifiableList(typeMappers);
    }

    public TypeMapper getTypeMapper(AnnotatedType javaType) {
        return typeMappers.stream()
                .filter(typeMapper -> typeMapper.supports(javaType))
                .findFirst()
                .orElseThrow(() -> new MappingException(String.format("No %s found for type %s",
                        TypeMapper.class.getSimpleName(), ClassUtils.toString(javaType))));
    }

    public AnnotatedType getMappableOutputType(AnnotatedType type) {
        TypeMapper mapper = this.getTypeMapper(type);
        if (mapper instanceof TypeSubstituter) {
            return getMappableOutputType(((TypeSubstituter) mapper).getSubstituteType(type));
        }
        return ClassUtils.transformType(type, this::getMappableOutputType);
    }
}
