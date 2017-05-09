package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Optional;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLTypeResolver;
import io.leangen.graphql.generator.exceptions.UnresolvableTypeException;
import io.leangen.graphql.generator.types.MappedGraphQLType;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.util.Utils;

public class DelegatingTypeResolver implements TypeResolver {

    private final TypeRepository typeRepository;
    private final TypeInfoGenerator typeInfoGenerator;
    private final String abstractTypeName;

    public DelegatingTypeResolver(TypeRepository typeRepository, TypeInfoGenerator typeInfoGenerator) {
        this(null, typeRepository, typeInfoGenerator);
    }

    public DelegatingTypeResolver(String abstractTypeName, TypeRepository typeRepository, TypeInfoGenerator typeInfoGenerator) {
        this.typeRepository = typeRepository;
        this.typeInfoGenerator = typeInfoGenerator;
        this.abstractTypeName = abstractTypeName;
    }

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        Object result = env.getObject();
        Class<?> resultType = result.getClass();
        String resultTypeName = typeInfoGenerator.generateTypeName(GenericTypeReflector.annotate(resultType));
        AnnotatedType returnType = ((MappedGraphQLType) env.getFieldType()).getJavaType();
        String abstractTypeName = this.abstractTypeName != null ? this.abstractTypeName : env.getFieldType().getName();

        //Check if the type is already unambiguous
        List<MappedType> mappedTypes = typeRepository.getOutputTypes(abstractTypeName, resultType);
        if (mappedTypes.isEmpty()) {
            return (GraphQLObjectType) env.getSchema().getType(resultTypeName);
        }
        if (mappedTypes.size() == 1) {
            return mappedTypes.get(0).graphQLType;
        }
        
        //Try to find an explicit resolver
        Optional<GraphQLObjectType> resolvedType = Utils.or(
                Optional.ofNullable(returnType.getAnnotation(GraphQLTypeResolver.class)),
                Optional.ofNullable(resultType.getAnnotation(GraphQLTypeResolver.class)))
                .map(ann -> resolveType(env, ann));
        if (resolvedType.isPresent()) {
            return resolvedType.get();
        }
        
        //Try to deduce the type
        if (resultTypeName.equals(env.getFieldType().getName())) {
            AnnotatedType resolvedJavaType = GenericTypeReflector.getExactSubType(returnType, resultType);
            if (resolvedJavaType != null && !GenericTypeReflector.isMissingTypeParameters(resolvedJavaType.getType())) {
                GraphQLType resolved = env.getSchema().getType(typeInfoGenerator.generateTypeName(resolvedJavaType));
                if (resolved == null) {
                    throw new UnresolvableTypeException(env.getFieldType().getName(), result);
                }
                return (GraphQLObjectType) resolved;
            }
        }
        
        //Give up
        throw new UnresolvableTypeException(env.getFieldType().getName(), result);
    }

    private GraphQLObjectType resolveType(TypeResolutionEnvironment env, GraphQLTypeResolver descriptor) {
        try {
            return descriptor.value().newInstance().resolveType(
                    new io.leangen.graphql.execution.TypeResolutionEnvironment(env, typeRepository, typeInfoGenerator));
        } catch (ReflectiveOperationException e) {
            throw new UnresolvableTypeException(env.getObject(), e);
        }
    }
}
