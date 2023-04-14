package io.leangen.graphql.generator.mapping;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.TypedElement;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;

public class TypeMappingEnvironment {

    public final TypedElement rootElement;
    public final List<AnnotatedType> typeStack;
    public final OperationMapper operationMapper;
    public final BuildContext buildContext;

    public TypeMappingEnvironment(TypedElement rootElement, OperationMapper operationMapper, BuildContext buildContext) {
        this.rootElement = rootElement;
        this.typeStack = new ArrayList<>();
        this.operationMapper = operationMapper;
        this.buildContext = buildContext;
    }

    public TypeMappingEnvironment forElement(TypedElement rootElement) {
        return new TypeMappingEnvironment(rootElement, operationMapper, buildContext);
    }

    public GraphQLOutputType toGraphQLType(AnnotatedType javaType) {
        return operationMapper.toGraphQLType(javaType, this);
    }

    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType) {
        return operationMapper.toGraphQLInputType(javaType, this);
    }

    public final TypeMappingEnvironment addType(AnnotatedType type) {
        if (!typeStack.contains(type)) {
            typeStack.add(type);
        }
        return this;
    }
}
