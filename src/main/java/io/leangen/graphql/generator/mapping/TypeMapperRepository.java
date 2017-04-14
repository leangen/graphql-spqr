package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

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
}
