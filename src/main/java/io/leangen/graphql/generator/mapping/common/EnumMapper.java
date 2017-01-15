package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.util.ClassUtils;

import static graphql.schema.GraphQLEnumType.newEnum;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class EnumMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(buildContext.typeMetaDataGenerator.generateTypeName(javaType))
                .description(buildContext.typeMetaDataGenerator.generateTypeDescription(javaType));
        addOptions(enumBuilder, ClassUtils.getRawType(javaType.getType()));
        return enumBuilder.build();
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(buildContext.typeMetaDataGenerator.generateInputTypeName(javaType))
                .description(buildContext.typeMetaDataGenerator.generateInputTypeDescription(javaType));
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
