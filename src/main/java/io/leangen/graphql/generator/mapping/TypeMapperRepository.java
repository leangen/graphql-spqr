package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.addAll;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class TypeMapperRepository {

    private final List<TypeMapper> typeMappers = new ArrayList<>();
    private final Map<String, TypeMapper> typeMappersByPath = new HashMap<>();

    public void registerTypeMapper(TypeMapper... typeMappers) {
        addAll(this.typeMappers, typeMappers);
    }

    public void registerTypeMapperForPath(String path, AnnotatedType javaType) {
        TypeMapper mapper = getTypeMapper(javaType);
        if (mapper != null) {
            typeMappersByPath.put(path, mapper);
        }
    }

    private TypeMapper getTypeMapper(AnnotatedType javaType) {
        return typeMappers.stream().filter(typeMapper -> typeMapper.supports(javaType)).findFirst().orElse(null);
    }

    public TypeMapper getTypeMapperByPath(String path) {
        return typeMappersByPath.get(path);
    }
}
