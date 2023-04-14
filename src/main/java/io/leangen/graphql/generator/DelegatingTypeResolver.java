package io.leangen.graphql.generator;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLTypeResolver;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.exceptions.UnresolvableTypeException;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Optional;

public class DelegatingTypeResolver implements TypeResolver {

    private final String abstractTypeName;
    private final GlobalEnvironment globalEnv;

    DelegatingTypeResolver(GlobalEnvironment environment) {
        this(null, environment);
    }

    DelegatingTypeResolver(String abstractTypeName, GlobalEnvironment environment) {
        this.abstractTypeName = abstractTypeName;
        this.globalEnv = environment;
    }

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        Object result = env.getObject();
        Class<?> resultType = result.getClass();
        String resultTypeName = globalEnv.typeInfoGenerator.generateTypeName(GenericTypeReflector.annotate(resultType), globalEnv.messageBundle);
        GraphQLNamedType fieldType = (GraphQLNamedType) env.getFieldType();
        String abstractTypeName = this.abstractTypeName != null ? this.abstractTypeName : fieldType.getName();

        //Check if the type is already unambiguous
        List<MappedType> mappedTypes = globalEnv.typeRegistry.getOutputTypes(abstractTypeName, resultType);
        if (mappedTypes.isEmpty()) {
            return (GraphQLObjectType) env.getSchema().getType(resultTypeName);
        }
        if (mappedTypes.size() == 1) {
            return mappedTypes.get(0).getAsObjectType();
        }

        AnnotatedType returnType = globalEnv.typeRegistry.getMappedType(fieldType);
        //Try to find an explicit resolver
        Optional<GraphQLObjectType> resolvedType = Utils.or(
                Optional.ofNullable(returnType != null ? returnType.getAnnotation(GraphQLTypeResolver.class) : null),
                Optional.ofNullable(resultType.getAnnotation(GraphQLTypeResolver.class)))
                .map(ann -> resolveType(env, ann));
        if (resolvedType.isPresent()) {
            return resolvedType.get();
        }

        //Try to deduce the type
        if (returnType != null) {
            AnnotatedType resolvedJavaType = GenericTypeReflector.getExactSubType(returnType, resultType);
            if (resolvedJavaType != null && !ClassUtils.isMissingTypeParameters(resolvedJavaType.getType())) {
                GraphQLType resolved = env.getSchema().getType(globalEnv.typeInfoGenerator.generateTypeName(resolvedJavaType, globalEnv.messageBundle));
                if (resolved == null) {
                    throw new UnresolvableTypeException(fieldType.getName(), result);
                }
                return (GraphQLObjectType) resolved;
            }
        }
        
        //Give up
        throw new UnresolvableTypeException(fieldType.getName(), result);
    }

    private GraphQLObjectType resolveType(TypeResolutionEnvironment env, GraphQLTypeResolver descriptor) {
        try {
            return ClassUtils.instanceWithOptionalInjection(descriptor.value(), globalEnv).getType(env);
        } catch (Exception e) {
            throw new UnresolvableTypeException(env.<Object>getObject(), e);
        }
    }
}
