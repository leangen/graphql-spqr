package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Set;

import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.strategy.type.TypeMetaDataGenerator;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class CachingAbstractAwareMapper<O extends GraphQLOutputType, I extends GraphQLInputType> implements TypeMapper {
    
    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        registerAbstract(javaType, abstractTypes, buildContext);
        
        String typeName = getTypeName(javaType, buildContext);
        if (buildContext.knownTypes.contains(typeName)) {
            return getReferenceFor(typeName);
        }
        buildContext.knownTypes.add(typeName);
        return toGraphQLType(typeName, javaType, abstractTypes, queryGenerator, buildContext);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        registerAbstract(javaType, abstractTypes, buildContext);

        String typeName = getInputTypeName(javaType, buildContext);
        if (buildContext.knownInputTypes.contains(typeName)) {
            return getInputReferenceFor(typeName);
        }
        buildContext.knownInputTypes.add(typeName);
        return toGraphQLInputType(typeName, javaType, abstractTypes, queryGenerator, buildContext);
    }
    
    protected abstract O toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext);

    protected abstract I toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext);
    
    protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, getTypeArguments(0), buildContext.typeMetaDataGenerator);
    }
    
    protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, getTypeArguments(1), buildContext.typeMetaDataGenerator);
    }
    
    protected GraphQLOutputType getReferenceFor(String name) {
        return referenceFor(getTypeArguments(0), name);
    }
    
    protected GraphQLInputType getInputReferenceFor(String name) {
        return inputReferenceFor(getTypeArguments(1), name);
    }
    
    protected void registerAbstract(AnnotatedType type, Set<Type> abstractTypes, BuildContext buildContext) {
        if (ClassUtils.isAbstract(type)) {
            abstractTypes.add(type.getType());
        }
    }
    
    private GraphQLOutputType referenceFor(AnnotatedType type, String typeName) {
        if (GenericTypeReflector.isSuperType(GraphQLObjectType.class, type.getType())) {
            return GraphQLObjectType.reference(typeName);
        }
        if (GenericTypeReflector.isSuperType(GraphQLInterfaceType.class, type.getType())) {
            return GraphQLInterfaceType.reference(typeName);
        }
        return new GraphQLTypeReference(typeName);
    }
    
    private GraphQLInputType inputReferenceFor(AnnotatedType graphQLType, String typeName) {
        if (GenericTypeReflector.isSuperType(GraphQLInputObjectType.class, graphQLType.getType())) {
            return GraphQLInputObjectType.reference(typeName);
        }
        return new GraphQLTypeReference(typeName);
    }
    
    private String getTypeName(AnnotatedType javaType, AnnotatedType graphQLType, TypeMetaDataGenerator metaDataGenerator) {
        if (GenericTypeReflector.isSuperType(GraphQLScalarType.class, graphQLType.getType())) {
            return metaDataGenerator.generateScalarTypeName(javaType);
        }
        if (GenericTypeReflector.isSuperType(GraphQLInputType.class, graphQLType.getType())) {
            return metaDataGenerator.generateInputTypeName(javaType);
        }
        return metaDataGenerator.generateTypeName(javaType);
    }
    
    private AnnotatedType getTypeArguments(int index) {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), CachingAbstractAwareMapper.class.getTypeParameters()[index]);
    }
}
