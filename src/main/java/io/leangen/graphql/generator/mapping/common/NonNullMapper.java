package io.leangen.graphql.generator.mapping.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collection;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.NonNull;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class NonNullMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        return new GraphQLNonNull(queryGenerator.toGraphQLType(removeNonNull(javaType), buildContext));
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        return new GraphQLNonNull(queryGenerator.toGraphQLInputType(removeNonNull(javaType), buildContext));
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(NonNull.class);
    }

    private AnnotatedType removeNonNull(AnnotatedType type) {
        Collection<Annotation> keptAnnotations = new ArrayList<>(type.getAnnotations().length - 1);
        for (Annotation annotation : type.getAnnotations()) {
            if (annotation.annotationType() != NonNull.class) {
                keptAnnotations.add(annotation);
            }
        }
        return GenericTypeReflector.replaceAnnotations(type, keptAnnotations.toArray(new Annotation[keptAnnotations.size()]));
    }
}
