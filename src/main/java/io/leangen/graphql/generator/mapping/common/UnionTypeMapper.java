package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.types.GraphQLUnion;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class UnionTypeMapper extends UnionMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLUnion annotation = javaType.getAnnotation(GraphQLUnion.class);
        List<AnnotatedType> possibleJavaTypes = getPossibleJavaTypes(javaType, buildContext);
        return toGraphQLUnion(annotation.name(), annotation.description(), javaType, possibleJavaTypes, abstractTypes, operationMapper, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
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
        if (possibleTypes.isEmpty() && annotation.possibleTypeAutoDiscovery()) {
            String[] scanPackages = annotation.scanPackages();
            if (scanPackages.length == 0 && Utils.arrayNotEmpty(buildContext.basePackages)) {
                scanPackages = buildContext.basePackages;
            }
            possibleTypes = ClassUtils.findImplementations(javaType, scanPackages).stream()
                    .peek(impl -> {
                        if (ClassUtils.isMissingTypeParameters(impl.getType())) {
                            throw new TypeMappingException(javaType.getType(), impl.getType());
                        }
                    })
                    .collect(Collectors.toList());
        }
        if (possibleTypes.isEmpty()) {
            throw new TypeMappingException("No possible types found for union type " + javaType.getType().getTypeName());
        }
        return possibleTypes;
    }
}
