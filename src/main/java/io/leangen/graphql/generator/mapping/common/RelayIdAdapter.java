package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Set;

import graphql.schema.GraphQLNonNull;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.util.Scalars;

/**
 * Maps, converts and injects global Relay spec compliant GraphQL IDs
 */
public class RelayIdAdapter extends IdAdapter {

    @Override
    public GraphQLNonNull graphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return Scalars.RelayId;
    }

    @Override
    public GraphQLNonNull graphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return Scalars.RelayId;
    }

    @Override
    public Object convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return resolutionEnvironment.globalEnvironment.relay.toGlobalId(resolutionEnvironment.parentType.getName(), resolutionEnvironment.valueMapper.toString(original));
    }

    @Override
    public Object getArgumentValue(Object input, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        if (input == null) {
            return null;
        }
        String rawId = input.toString();
        String id = rawId;
        try {
            id = resolutionEnvironment.globalEnvironment.relay.fromGlobalId(rawId).getId();
        } catch (Exception e) {/*no-op*/}
        return resolutionEnvironment.valueMapper.fromString(id, type);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(RelayId.class);
    }
}
