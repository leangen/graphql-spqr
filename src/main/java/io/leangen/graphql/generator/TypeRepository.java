package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import graphql.schema.GraphQLObjectType;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class TypeRepository {

    private Map<String, Set<MappedType>> covariantOutputTypes = new ConcurrentHashMap<>();

    public void registerCovariantTypes(String compositeTypeName, AnnotatedType javaSubType, GraphQLObjectType subType) {
        this.covariantOutputTypes.putIfAbsent(compositeTypeName, new HashSet<>());
        this.covariantOutputTypes.get(compositeTypeName).add(new MappedType(javaSubType, subType));
    }

    public List<MappedType> getOutputTypes(String compositeTypeName, Class objectType) {
        Set<MappedType> mappedTypes = this.covariantOutputTypes.get(compositeTypeName);
        if (mappedTypes == null) return Collections.emptyList();
        if (objectType == null) return new ArrayList<>(mappedTypes);
        return mappedTypes.stream()
                .filter(mappedType -> ClassUtils.getRawType(mappedType.javaType.getType()).isAssignableFrom(objectType))
                .collect(Collectors.toList());
    }

    public List<MappedType> getOutputTypes(String compositeTypeName) {
        return new ArrayList<>(this.covariantOutputTypes.get(compositeTypeName));
    }
}
