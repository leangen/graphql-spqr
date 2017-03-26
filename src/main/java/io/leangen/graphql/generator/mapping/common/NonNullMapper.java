package io.leangen.graphql.generator.mapping.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class NonNullMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper OperationMapper, BuildContext buildContext) {
        return new graphql.schema.GraphQLNonNull(OperationMapper.toGraphQLType(removeNonNull(javaType), abstractTypes, buildContext));
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper OperationMapper, BuildContext buildContext) {
        return new graphql.schema.GraphQLNonNull(OperationMapper.toGraphQLInputType(removeNonNull(javaType), abstractTypes, buildContext));
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLNonNull.class);
    }

    private AnnotatedType removeNonNull(AnnotatedType type) {
        Collection<Annotation> keptAnnotations = new ArrayList<>(type.getAnnotations().length - 1);
        for (Annotation annotation : type.getAnnotations()) {
            if (annotation.annotationType() != GraphQLNonNull.class) {
                keptAnnotations.add(annotation);
            }
        }
        return GenericTypeReflector.replaceAnnotations(type, keptAnnotations.toArray(new Annotation[keptAnnotations.size()]));
    }
}
