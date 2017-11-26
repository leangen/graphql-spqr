package io.leangen.graphql.generator.mapping.core;

import java.lang.reflect.AnnotatedType;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import graphql.schema.GraphQLInputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.leangen.graphql.util.ClassUtils;

public class CompletableFutureMapper extends AbstractTypeSubstitutingMapper {

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        throw new UnsupportedOperationException(CompletableFuture.class.getSimpleName() + " can not be used as an input type");
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType innerType = GenericTypeReflector.getTypeParameter(original, CompletableFuture.class.getTypeParameters()[0]);
        return ClassUtils.addAnnotations(innerType, original.getAnnotations());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(CompletableFuture.class, type.getType());
    }
}
