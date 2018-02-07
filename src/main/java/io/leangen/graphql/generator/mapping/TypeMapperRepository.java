package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

import io.leangen.graphql.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class TypeMapperRepository {

    private final List<TypeMapper> typeMappers;

    public TypeMapperRepository(List<TypeMapper> typeMappers) {
        this.typeMappers = Collections.unmodifiableList(typeMappers);
    }

    public TypeMapper getTypeMapper(AnnotatedType javaType) {
        return typeMappers.stream().filter(typeMapper -> typeMapper.supports(javaType)).findFirst().orElse(null);
    }

    public AnnotatedType getMappableOutputType(AnnotatedType type) {
        TypeMapper mapper = this.getTypeMapper(type);
        if (mapper instanceof AbstractTypeSubstitutingMapper) {
            return getMappableOutputType(((AbstractTypeSubstitutingMapper) mapper).getSubstituteType(type));
        }
        return ClassUtils.transformType(type, this::getMappableOutputType);
    }
}
