package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.types.GraphQLUnion;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class UnionTypeMapper extends UnionMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        GraphQLUnion annotation = javaType.getAnnotation(GraphQLUnion.class);
        List<AnnotatedType> possibleJavaTypes = getPossibleJavaTypes(javaType, env.buildContext);
        final String name = env.buildContext.typeInfoGenerator.generateTypeName(javaType, env.buildContext.messageBundle);
        return toGraphQLUnion(name, annotation.description(), javaType, possibleJavaTypes, env);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLUnion.class)
                || ClassUtils.getRawType(type.getType()).isAnnotationPresent(GraphQLUnion.class);
    }

    private List<AnnotatedType> getPossibleJavaTypes(AnnotatedType javaType, BuildContext buildContext) {
        GraphQLUnion annotation = javaType.getAnnotation(GraphQLUnion.class);
        List<AnnotatedType> possibleTypes = Collections.emptyList();
        if (annotation.possibleTypes().length > 0) {
            possibleTypes = Arrays.stream(annotation.possibleTypes())
                    .map(type -> GenericTypeReflector.getExactSubType(javaType, type))
                    .collect(Collectors.toList());
        }
        if (possibleTypes.isEmpty()) {
            try {
                possibleTypes = annotation.possibleTypeFactory().newInstance().getPossibleTypes();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(annotation.possibleTypeFactory().getName() +
                        " must have a public default constructor", e);
            }
        }
        if (possibleTypes.isEmpty()) {
            possibleTypes = buildContext.implDiscoveryStrategy.findImplementations(javaType, annotation.possibleTypeAutoDiscovery(), annotation.scanPackages(), buildContext);
        }
        if (possibleTypes.isEmpty()) {
            throw new TypeMappingException("No possible types found for union type " + javaType.getType().getTypeName());
        }
        return possibleTypes;
    }
}
