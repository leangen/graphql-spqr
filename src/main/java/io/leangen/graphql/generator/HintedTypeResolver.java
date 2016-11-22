package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.naming.OperationNotSupportedException;

import graphql.GraphQLException;
import graphql.TypeResolutionEnvironment;
import graphql.language.Field;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLTypeHintProvider;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.proxy.TypeHintProvider;
import io.leangen.graphql.generator.types.MappedGraphQLType;
import io.leangen.graphql.metadata.DomainType;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class HintedTypeResolver implements TypeResolver {

    private final TypeRepository typeRepository;
    private final TypeMapperRepository typeMappers;

    public HintedTypeResolver(TypeRepository typeRepository, TypeMapperRepository typeMappers) {
        this.typeRepository = typeRepository;
        this.typeMappers = typeMappers;
    }

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        try {
            return getGraphQLType(env);
        } catch (Exception e) {
            throw new GraphQLException(e);
        }
    }

    public GraphQLObjectType getGraphQLType(TypeResolutionEnvironment env) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Object result = env.getObject();
        AnnotatedType returnType = ((MappedGraphQLType) env.getFieldType()).getJavaType();
        if (env.getField().getSelectionSet().getSelections().stream().allMatch(s -> s.getClass() == Field.class)) {
            return null;
        }
        Class<?> type = result.getClass();

        List<MappedType> mappedTypes = typeRepository.getOutputTypes(env.getFieldType().getName(), type);
        if (mappedTypes.isEmpty()) {
            return (GraphQLObjectType) env.getSchema().getType(new DomainType(GenericTypeReflector.annotate(type)).getName());
        }

        if (mappedTypes.size() == 1) {
            return (GraphQLObjectType) typeRepository.getOutputTypes(env.getFieldType().getName(), result.getClass()).get(0).graphQLType;
        }
        if (returnType.isAnnotationPresent(GraphQLTypeHintProvider.class)) {
            TypeHintProvider hint = returnType.getAnnotation(GraphQLTypeHintProvider.class).value().newInstance();
            return (GraphQLObjectType) env.getSchema().getType(hint.getGraphQLTypeHint(result, env));
        } else if (type.isAnnotationPresent(GraphQLTypeHintProvider.class)) {
            TypeHintProvider hint = type.getAnnotation(GraphQLTypeHintProvider.class).value().newInstance();
            return (GraphQLObjectType) env.getSchema().getType(hint.getGraphQLTypeHint(result, env));
        } else if (ClassUtils.getRawType(returnType.getType()).isAnnotationPresent(GraphQLTypeHintProvider.class)) {
            TypeHintProvider hint = result.getClass().getAnnotation(GraphQLTypeHintProvider.class).value().newInstance();
            return (GraphQLObjectType) env.getSchema().getType(hint.getGraphQLTypeHint(result, env));
        } else
        if (new DomainType(GenericTypeReflector.annotate(type)).getName().equals(env.getFieldType().getName())) {
            try {
                AnnotatedType resolved = GenericTypeReflector.getExactSubType(returnType, type);
                if (resolved != null) {
                    return (GraphQLObjectType) env.getSchema().getType(new DomainType(resolved).getName());
                }
            } catch (Exception e) {/*no-op*/}
        }
        throw new IllegalStateException(String.format(
                "Exact GraphQL type for %s is unresolvable for object of type %s",
                env.getFieldType().getName(), result.getClass().getCanonicalName()));
    }

    @Override
    public GraphQLObjectType getType(Object object) {
        throw new GraphQLException(new OperationNotSupportedException("Simple type resolution not supported"));
    }
}
