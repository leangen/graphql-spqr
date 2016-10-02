package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.addAll;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class TypeMapperRepository {

    private final List<TypeMapper> typeMappers = new ArrayList<>();

    public void registerTypeMappers(TypeMapper... typeMappers) {
        addAll(this.typeMappers, typeMappers);
    }

    public void registerTypeMappersWithPriority(TypeMapper... typeMappers) {
        List<TypeMapper> mappers = Arrays.asList(typeMappers);
        Collections.reverse(mappers);
        mappers.forEach(mapper -> this.typeMappers.add(0, mapper));
    }

    public TypeMapper getTypeMapper(AnnotatedType javaType) {
        return typeMappers.stream().filter(typeMapper -> typeMapper.supports(javaType)).findFirst().orElse(null);
    }

    public boolean isEmpty() {
        return typeMappers.isEmpty();
    }
}
