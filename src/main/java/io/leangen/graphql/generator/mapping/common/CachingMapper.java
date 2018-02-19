package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class CachingMapper<O extends GraphQLOutputType, I extends GraphQLInputType> implements TypeMapper {
    
    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        String typeName = getTypeName(javaType, buildContext);
        if (buildContext.isKnownType(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.registerTypeName(typeName);
        return toGraphQLType(typeName, javaType, operationMapper, buildContext);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        String typeName = getInputTypeName(javaType, buildContext);
        if (buildContext.isKnownInputType(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.registerInputTypeName(typeName);
        return toGraphQLInputType(typeName, javaType, operationMapper, buildContext);
    }
    
    protected abstract O toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext);

    protected abstract I toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext);
    
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
