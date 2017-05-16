package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Set;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class CachingMapper<O extends GraphQLOutputType, I extends GraphQLInputType> extends AbstractionCollectingMapper {
    
    @Override
    public GraphQLOutputType graphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        String typeName = getTypeName(javaType, buildContext);
        if (buildContext.knownTypes.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.knownTypes.add(typeName);
        return toGraphQLType(typeName, javaType, abstractTypes, operationMapper, buildContext);
    }

    @Override
    public GraphQLInputType graphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        String typeName = getInputTypeName(javaType, buildContext);
        if (buildContext.knownInputTypes.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.knownInputTypes.add(typeName);
        return toGraphQLInputType(typeName, javaType, abstractTypes, operationMapper, buildContext);
    }
    
    protected abstract O toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext);

    protected abstract I toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext);
    
    protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, getTypeArguments(0), buildContext.typeInfoGenerator);
    }
    
    protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, getTypeArguments(1), buildContext.typeInfoGenerator);
    }
    
    private String getTypeName(AnnotatedType javaType, AnnotatedType graphQLType, TypeInfoGenerator typeInfoGenerator) {
        if (GenericTypeReflector.isSuperType(GraphQLScalarType.class, graphQLType.getType())) {
            return typeInfoGenerator.generateScalarTypeName(javaType);
        }
        if (GenericTypeReflector.isSuperType(GraphQLInputType.class, graphQLType.getType())) {
            return typeInfoGenerator.generateInputTypeName(javaType);
        }
        return typeInfoGenerator.generateTypeName(javaType);
    }
    
    private AnnotatedType getTypeArguments(int index) {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), CachingMapper.class.getTypeParameters()[index]);
    }
}
