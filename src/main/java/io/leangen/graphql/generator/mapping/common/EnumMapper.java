package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.Arrays;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.DomainType;
import io.leangen.graphql.util.ClassUtils;

import static graphql.schema.GraphQLEnumType.newEnum;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class EnumMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        DomainType enumType = new DomainType(javaType);
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(enumType.getName())
                .description(enumType.getDescription());
        addOptions(enumBuilder, ClassUtils.getRawType(javaType.getType()));
        return enumBuilder.build();
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        DomainType enumType = new DomainType(javaType);
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(enumType.getInputName())
                .description(enumType.getDescription());
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
