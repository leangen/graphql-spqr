package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class CachingMapper<O extends GraphQLOutputType, I extends GraphQLInputType> implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        String typeName = getTypeName(javaType, env.buildContext);
        if (env.buildContext.typeCache.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        env.buildContext.typeCache.register(typeName);
        return toGraphQLType(typeName, javaType, env);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        String typeName = getInputTypeName(javaType, env.buildContext);
        if (env.buildContext.typeCache.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        env.buildContext.typeCache.register(typeName);
        return toGraphQLInputType(typeName, javaType, env);
    }

    protected abstract O toGraphQLType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env);

    protected abstract I toGraphQLInputType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env);

    protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, getTypeArguments(0), buildContext.typeInfoGenerator, buildContext.messageBundle);
    }

    protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, getTypeArguments(1), buildContext.typeInfoGenerator, buildContext.messageBundle);
    }

    private String getTypeName(AnnotatedType javaType, AnnotatedType graphQLType, TypeInfoGenerator typeInfoGenerator, MessageBundle messageBundle) {
        if (ClassUtils.isSuperClass(GraphQLScalarType.class, graphQLType)) {
            return typeInfoGenerator.generateScalarTypeName(javaType, messageBundle);
        }
        if (ClassUtils.isSuperClass(GraphQLEnumType.class, graphQLType)) {
            return typeInfoGenerator.generateEnumTypeName(javaType, messageBundle);
        }
        if (ClassUtils.isSuperClass(GraphQLInputType.class, graphQLType)) {
            return typeInfoGenerator.generateInputTypeName(javaType, messageBundle);
        }
        return typeInfoGenerator.generateTypeName(javaType, messageBundle);
    }

    private AnnotatedType getTypeArguments(int index) {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), CachingMapper.class.getTypeParameters()[index]);
    }
}
