package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;

import graphql.schema.GraphQLEnumType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.util.ClassUtils;

import static graphql.schema.GraphQLEnumType.newEnum;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class EnumMapper extends CachingMapper<GraphQLEnumType, GraphQLEnumType> {

    @Override
    public GraphQLEnumType toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType));
        addOptions(enumBuilder, ClassUtils.getRawType(javaType.getType()));
        return enumBuilder.build();
    }

    @Override
    public GraphQLEnumType toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType));
        addOptions(enumBuilder, ClassUtils.getRawType(javaType.getType()));
        return enumBuilder.build();
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.getRawType(type.getType()).isEnum();
    }

    private void addOptions(GraphQLEnumType.Builder enumBuilder, Class<?> enumClass) {
        Arrays.stream(enumClass.getEnumConstants()).forEach(
                enumConst -> enumBuilder.value(((Enum) enumConst).name(), enumConst));
    }
}
