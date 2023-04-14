package io.leangen.graphql.generator.mapping.common;

import graphql.Scalars;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Set;

import static io.leangen.graphql.util.Scalars.RelayId;

/**
 * Maps, converts and injects GraphQL IDs.
 * Despite implementing {@link InputConverter} it can't actually be used as such due to the lack of support for
 * {@code AnnotatedType} in any JSON library currently available
 */
public class IdAdapter implements TypeMapper, ArgumentInjector, OutputConverter<@GraphQLId Object, String>, InputConverter<@GraphQLId Object, String> {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return javaType.getAnnotation(GraphQLId.class).relayId() ? RelayId : Scalars.GraphQLID;
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return javaType.getAnnotation(GraphQLId.class).relayId() ? RelayId : Scalars.GraphQLID;
    }

    @Override
    public String convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        final String id = resolutionEnvironment.valueMapper.toString(original, type);
        if (type.getAnnotation(GraphQLId.class).relayId()) {
            return resolutionEnvironment.globalEnvironment.relay.toGlobalId(resolutionEnvironment.parentType.getName(), id);
        }
        return id;
    }

    @Override
    public Object convertInput(String substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        String id = substitute;
        if (type.getAnnotation(GraphQLId.class).relayId()) {
            try {
                id = environment.relay.fromGlobalId(id).getId();
            } catch (Exception e) {/*no-op*/}
        }
        return valueMapper.fromString(id, type);
    }

    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        if (params.getInput() == null) {
            return null;
        }
        ResolutionEnvironment env = params.getResolutionEnvironment();
        return convertInput(params.getInput().toString(), params.getType(), env.globalEnvironment, env.valueMapper);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLId.class);
    }

    @Override
    public boolean supports(AnnotatedType type, Parameter parameter) {
        return type.isAnnotationPresent(GraphQLId.class) || (parameter != null && parameter.isAnnotationPresent(GraphQLId.class));
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return supports(type);
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return GenericTypeReflector.annotate(String.class);
    }
}
