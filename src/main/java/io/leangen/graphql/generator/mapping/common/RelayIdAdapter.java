package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import graphql.Scalars;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.InputValueProvider;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.query.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RelayIdAdapter extends AbstractionCollectingMapper implements InputValueProvider, OutputConverter {

    @Override
    public GraphQLOutputType graphQLType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        return Scalars.GraphQLID;
    }

    @Override
    public GraphQLInputType graphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        return Scalars.GraphQLID;
    }

    @Override
    protected void registerAbstract(AnnotatedType type, Set<Type> abstractTypes, BuildContext buildContext) {
        abstractTypes.addAll(collectAbstract(type, new HashSet<>(), buildContext));
    }
    
    @Override
    public Object convertOutput(Object original, AnnotatedType type, ResolutionContext resolutionContext) {
        return resolutionContext.globalContext.relay.toGlobalId(resolutionContext.parentType.getName(), resolutionContext.valueMapper.toString(original));
    }

    @Override
    public Object getInputValue(Object input, AnnotatedType type, ResolutionContext resolutionContext) {
        String rawId = input.toString();
        String id = rawId;
        try {
            id = resolutionContext.globalContext.relay.fromGlobalId(rawId).getId();
        } catch (Exception e) {/*no-op*/}
        return resolutionContext.valueMapper.fromString(id, type);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(RelayId.class);
    }
}
