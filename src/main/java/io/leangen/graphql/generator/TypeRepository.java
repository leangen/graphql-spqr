package io.leangen.graphql.generator;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class TypeRepository {

    private Map<String, GraphQLOutputType> outputTypesByName = new HashMap<>();
    private Map<String, Set<MappedType>> covariantOutputTypes = new HashMap<>();
    private Map<Type, GraphQLInputType> inputTypesByClass = new HashMap<>();

    public void registerType(GraphQLOutputType type) {
        this.outputTypesByName.put(type.getName(), type);
    }

    public void registerCovariantTypes(String compositeTypeName, Type javaSubType, GraphQLOutputType subType) {
        this.covariantOutputTypes.putIfAbsent(compositeTypeName, new HashSet<>());
        this.covariantOutputTypes.get(compositeTypeName).add(new MappedType(javaSubType, subType));
    }

    public void registerCovariantTypes(Collection<String> compositeTypeNames, Type javaSubType, GraphQLOutputType subType) {
        compositeTypeNames.forEach(typeName -> registerCovariantTypes(typeName, javaSubType, subType));
    }

    public void registerType(Type javaType, GraphQLInputType type) {
        this.inputTypesByClass.put(javaType, type);
    }

    public GraphQLOutputType getOutputType(String name) {
        return outputTypesByName.get(name);
    }

    public Optional<GraphQLInputType> getInputType(Type javaType) {
        return Optional.ofNullable(inputTypesByClass.get(javaType));
    }

    public List<MappedType> getOutputTypes(String compositeTypeName, Class objectType) {
        Set<MappedType> mappedTypes = this.covariantOutputTypes.get(compositeTypeName);
        if (mappedTypes == null) return Collections.emptyList();
        return mappedTypes.stream()
                .filter(mappedType -> GenericTypeReflector.isSuperType(ClassUtils.getRawType(mappedType.javaType), objectType))
                .collect(Collectors.toList());
    }

    public class MappedType {
        public Type javaType;
        public GraphQLOutputType graphQLType;

        public MappedType(Type javaType, GraphQLOutputType graphQLType) {
            this.javaType = javaType;
            this.graphQLType = graphQLType;
        }
    }
}
