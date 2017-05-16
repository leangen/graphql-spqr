package io.leangen.graphql.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import io.leangen.graphql.generator.types.MappedGraphQLObjectType;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class TypeRepository {

    private final Map<String, Map<String, MappedType>> covariantOutputTypes = new ConcurrentHashMap<>();
    private final Map<String, GraphQLObjectType> knownObjectTypes = new HashMap<>();

    private final Logger log = LoggerFactory.getLogger(TypeRepository.class);
    
    public TypeRepository(Set<GraphQLType> knownTypes) {
        //extract known interface implementations
        knownTypes.stream()
                .filter(type -> type instanceof MappedGraphQLObjectType)
                .map(type -> (MappedGraphQLObjectType) type)
                .forEach(obj -> obj.getInterfaces().forEach(
                        inter -> registerCovariantTypes(inter.getName(), obj.getJavaType(), obj)));

        //extract known union members
        knownTypes.stream()
                .filter(type -> type instanceof GraphQLUnionType)
                .map(type -> (GraphQLUnionType) type)
                .forEach(union -> union.getTypes().stream()
                        .filter(type -> type instanceof MappedGraphQLObjectType)
                        .map(type -> (MappedGraphQLObjectType) type)
                        .forEach(obj -> registerCovariantTypes(union.getName(), obj.getJavaType(), obj)));
    }

    public void registerObjectType(GraphQLObjectType objectType) {
        this.knownObjectTypes.put(objectType.getName(), objectType);
    }

    public void registerCovariantTypes(String compositeTypeName, AnnotatedType javaSubType, GraphQLOutputType subType) {
        this.covariantOutputTypes.putIfAbsent(compositeTypeName, new ConcurrentHashMap<>());
        Map<String, MappedType> covariantTypes = this.covariantOutputTypes.get(compositeTypeName);
        //never overwrite an exact type with a reference
        if (subType instanceof GraphQLObjectType || covariantTypes.get(subType.getName()) == null || covariantTypes.get(subType.getName()).graphQLType instanceof GraphQLTypeReference) {
            covariantTypes.put(subType.getName(), new MappedType(javaSubType, subType));
        }
    }

    public List<MappedType> getOutputTypes(String compositeTypeName, Class objectType) {
        Map<String, MappedType> mappedTypes = this.covariantOutputTypes.get(compositeTypeName);
        if (mappedTypes == null) return Collections.emptyList();
        if (objectType == null) return new ArrayList<>(mappedTypes.values());
        return mappedTypes.values().stream()
                .filter(mappedType -> ClassUtils.getRawType(mappedType.javaType.getType()).isAssignableFrom(objectType))
                .collect(Collectors.toList());
    }

    public List<MappedType> getOutputTypes(String compositeTypeName) {
        return new ArrayList<>(this.covariantOutputTypes.get(compositeTypeName).values());
    }

    public void replaceTypeReferences() {
        for (Map<String, MappedType> covariantTypes : this.covariantOutputTypes.values()) {
            Set<String> toRemove = new HashSet<>();
            for (Map.Entry<String, MappedType> entry : covariantTypes.entrySet()) {
                if (entry.getValue().graphQLType instanceof GraphQLTypeReference) {
                    GraphQLObjectType resolvedType = knownObjectTypes.get(entry.getKey());
                    if (resolvedType != null) {
                        entry.setValue(new MappedType(entry.getValue().javaType, resolvedType));
                    } else {
                        log.warn("Type reference " + entry.getKey() + " could not replaced correctly. " +
                                "This can occur when the schema generator is initialized with " +
                                "additional types not built by GraphQL-SPQR. If this type implements " +
                                "Node, in some edge cases it may end up not exposed via the 'node' query.");
                        toRemove.add(entry.getKey());
                    }
                }
            }
            toRemove.forEach(covariantTypes::remove);
            covariantTypes.replaceAll((typeName, mapped) -> mapped.graphQLType instanceof GraphQLTypeReference
                    ? new MappedType(mapped.javaType, knownObjectTypes.get(typeName)) : mapped);
        }
        knownObjectTypes.clear();
    }
}
