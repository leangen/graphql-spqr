package io.leangen.graphql.generator;

import graphql.schema.*;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.leangen.graphql.util.GraphQLUtils.name;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class TypeRegistry {

    private final Map<String, Map<String, MappedType>> covariantOutputTypes = new ConcurrentHashMap<>();
    private final Set<GraphQLObjectType> discoveredTypes = new HashSet<>();
    private final Map<String, AnnotatedType> mappedTypes = new ConcurrentHashMap<>();
    private final Map<FieldCoordinates, Operation> mappedOperations = new ConcurrentHashMap<>();
    private final Map<FieldCoordinates, InputField> mappedInputFields = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(TypeRegistry.class);

    //TODO Register all type mappings as well!!!
    public TypeRegistry(Map<GraphQLNamedType, AnnotatedType> knownTypes) {
        knownTypes.entrySet().stream()
                .filter(mapping -> mapping.getValue() != null)
                .forEach(mapping -> this.mappedTypes.put(mapping.getKey().getName(), mapping.getValue()));
        //extract known interface implementations
        knownTypes.keySet().stream()
                .filter(type -> type instanceof GraphQLObjectType && isMappedType(type.getName()))
                .map(type -> (GraphQLObjectType) type)
                .forEach(obj -> obj.getInterfaces().forEach(
                        inter -> registerCovariantType(inter.getName(), getMappedType(obj), obj)));

        //extract known union members
        knownTypes.keySet().stream()
                .filter(type -> type instanceof GraphQLUnionType)
                .map(type -> (GraphQLUnionType) type)
                .forEach(union -> union.getTypes().stream()
                        .filter(type -> type instanceof GraphQLObjectType && isMappedType(type.getName()))
                        .map(type -> (GraphQLObjectType) type)
                        .forEach(obj -> registerCovariantType(union.getName(), getMappedType(obj), obj)));
    }

    public void registerDiscoveredCovariantType(String compositeTypeName, AnnotatedType javaSubType, GraphQLObjectType subType) {
        this.discoveredTypes.add(subType);
        registerCovariantType(compositeTypeName, javaSubType, subType);
    }
    
    public void registerCovariantType(String compositeTypeName, AnnotatedType javaSubType, GraphQLNamedOutputType subType) {
        this.covariantOutputTypes.putIfAbsent(compositeTypeName, new ConcurrentHashMap<>());
        Map<String, MappedType> covariantTypes = this.covariantOutputTypes.get(compositeTypeName);
        //never overwrite an exact type with a reference
        if (subType instanceof GraphQLObjectType || covariantTypes.get(subType.getName()) == null || covariantTypes.get(subType.getName()).graphQLType instanceof GraphQLTypeReference) {
            covariantTypes.put(subType.getName(), new MappedType(javaSubType, subType));
        }
    }

    public void registerMapping(String typeName, AnnotatedType javaType) {
        this.mappedTypes.put(typeName, javaType);
    }

    public void registerMapping(FieldCoordinates field, Operation operation) {
        this.mappedOperations.put(field, operation);
    }

    public void registerMapping(FieldCoordinates field, InputField inputField) {
        this.mappedInputFields.put(field, inputField);
    }

    @SuppressWarnings("WeakerAccess")
    public List<MappedType> getOutputTypes(String compositeTypeName, Class<?> objectType) {
        Map<String, MappedType> mappedTypes = this.covariantOutputTypes.get(compositeTypeName);
        if (mappedTypes == null) return Collections.emptyList();
        if (objectType == null) return new ArrayList<>(mappedTypes.values());
        return mappedTypes.values().stream()
                .filter(mappedType -> mappedType.rawJavaType.isAssignableFrom(objectType))
                .collect(Collectors.toList());
    }

    public List<MappedType> getOutputTypes(String compositeTypeName) {
        return new ArrayList<>(this.covariantOutputTypes.get(compositeTypeName).values());
    }

    public Set<GraphQLObjectType> getDiscoveredTypes() {
        return discoveredTypes;
    }

    public AnnotatedType getMappedType(GraphQLNamedType type) {
        AnnotatedType mappedType = this.mappedTypes.get(type.getName());
        if (mappedType == null) {
            throw new IllegalArgumentException("GraphQL type " + name(type) + " does not have a mapped Java type");
        }
        return mappedType;
    }

    public Map<String, AnnotatedType> getMappedTypes() {
        return mappedTypes;
    }

    public boolean isMappedType(String typeName) {
        return this.mappedTypes.containsKey(typeName);
    }

    public boolean isMappedType(GraphQLNamedType type) {
        return isMappedType(type.getName());
    }

    public Operation getMappedOperation(FieldCoordinates field) {
        return this.mappedOperations.get(field);
    }

    public Resolver getMappedResolver(FieldCoordinates field, Set<String> argumentNames) {
        Operation operation = getMappedOperation(field);
        return operation != null ? operation.getApplicableResolver(argumentNames) : null;
    }

    public InputField getMappedInputField(FieldCoordinates inputField) {
        return this.mappedInputFields.get(inputField);
    }

    void resolveTypeReferences(Map<String, GraphQLNamedType> resolvedTypes) {
        for (Map<String, MappedType> covariantTypes : this.covariantOutputTypes.values()) {
            Set<String> toRemove = new HashSet<>();
            for (Map.Entry<String, MappedType> entry : covariantTypes.entrySet()) {
                if (entry.getValue().graphQLType instanceof GraphQLTypeReference) {
                    GraphQLOutputType resolvedType = (GraphQLNamedOutputType) resolvedTypes.get(entry.getKey());
                    if (resolvedType != null) {
                        entry.setValue(new MappedType(entry.getValue().javaType, resolvedType));
                    } else {
                        log.warn("Type reference " + entry.getKey() + " could not be replaced correctly. " +
                                "This can occur when the schema generator is initialized with " +
                                "additional types not built by GraphQL SPQR. If this type implements " +
                                "Node, in some edge cases it may end up not exposed via the 'node' query.");
                        //the edge case is when the primary resolver returns an interface or a union and not the node type directly
                        toRemove.add(entry.getKey());
                    }
                }
            }
            toRemove.forEach(covariantTypes::remove);
            covariantTypes.replaceAll((typeName, mapped) -> mapped.graphQLType instanceof GraphQLTypeReference
                    ? new MappedType(mapped.javaType, (GraphQLOutputType) resolvedTypes.get(typeName)) : mapped);
        }
    }
}
