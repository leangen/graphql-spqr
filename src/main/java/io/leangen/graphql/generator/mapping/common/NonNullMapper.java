package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
@GraphQLIgnore
public class NonNullMapper implements TypeMapper, Comparator<AnnotatedType> {

    private final Set<Class<? extends Annotation>> nonNullAnnotations;

    @SuppressWarnings("unchecked")
    public NonNullMapper() {
        Set<Class<? extends Annotation>> annotations = new HashSet<>();
        annotations.add(io.leangen.graphql.annotations.GraphQLNonNull.class);
        for (String additional : new String[] {"javax.annotation.Nonnull", "javax.validation.constraints.NotNull", "org.jetbrains.annotations.NotNull"}) {
            try {
                annotations.add((Class<? extends Annotation>) ClassUtils.forName(additional));
            } catch (ClassNotFoundException e) {
                /*no-op*/
            }
        }
        this.nonNullAnnotations = Collections.unmodifiableSet(annotations);
    }

    @Override
    public GraphQLNonNull toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        mappersToSkip.add(this.getClass());
        GraphQLOutputType inner = operationMapper.toGraphQLType(javaType, mappersToSkip, buildContext);
        return inner instanceof GraphQLNonNull ? (GraphQLNonNull) inner : new GraphQLNonNull(inner);
    }

    @Override
    public GraphQLNonNull toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        mappersToSkip.add(this.getClass());
        GraphQLInputType inner = operationMapper.toGraphQLInputType(javaType, mappersToSkip, buildContext);
        return inner instanceof GraphQLNonNull ? (GraphQLNonNull) inner : new GraphQLNonNull(inner);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return nonNullAnnotations.stream().anyMatch(type::isAnnotationPresent) || ClassUtils.getRawType(type.getType()).isPrimitive();
    }

    @Override
    public int compare(AnnotatedType o1, AnnotatedType o2) {
        return ClassUtils.removeAnnotations(o1, nonNullAnnotations).equals(ClassUtils.removeAnnotations(o2, nonNullAnnotations)) ? 0 : -1;
    }
}
