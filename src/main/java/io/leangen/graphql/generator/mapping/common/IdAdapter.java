package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import graphql.Scalars;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import static io.leangen.graphql.util.Scalars.RelayId;

/**
 * Maps, converts and injects GraphQL IDs
 */
public class IdAdapter extends AbstractionCollectingMapper implements ArgumentInjector, OutputConverter<@GraphQLId Object, String>, InputConverter<@GraphQLId Object, String> {

    @Override
    public GraphQLOutputType graphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return javaType.getAnnotation(GraphQLId.class).relayId() ? RelayId : Scalars.GraphQLID;
    }

    @Override
    public GraphQLInputType graphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return javaType.getAnnotation(GraphQLId.class).relayId() ? RelayId : Scalars.GraphQLID;
    }

    @Override
    protected void registerAbstract(AnnotatedType type, Set<Type> abstractTypes, BuildContext buildContext) {
        abstractTypes.addAll(collectAbstract(type, new HashSet<>(), buildContext));
    }

    @Override
    public String convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        if (type.getAnnotation(GraphQLId.class).relayId()) {
            return resolutionEnvironment.globalEnvironment.relay.toGlobalId(resolutionEnvironment.parentType.getName(), resolutionEnvironment.valueMapper.toString(original));
        }
        return resolutionEnvironment.valueMapper.toString(original);
    }

    @Override
    public Object convertInput(String substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        String id = substitute;
        if (type.getAnnotation(GraphQLId.class).relayId()) {
            try {
                id = environment.relay.fromGlobalId(id).getId();
            } catch (Exception e) {/*no-op*/}
        }
        if (type.getType().equals(String.class)) {
            id = "\"" + id + "\"";
        }
        return valueMapper.fromString(id, type);
    }

    @Override
    public Object getArgumentValue(Object input, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        if (input == null) {
            return null;
        }
        return convertInput(input.toString(), type, resolutionEnvironment.globalEnvironment, resolutionEnvironment.valueMapper);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLId.class);
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return GenericTypeReflector.annotate(String.class);
    }
}
