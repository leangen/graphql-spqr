package io.leangen.graphql.query;

import java.util.List;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.TypeResolver;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.generator.proxy.RelayNodeProxy;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class HintedTypeResolver implements TypeResolver {

    private TypeRepository typeRepository;

    public HintedTypeResolver(TypeRepository typeRepository) {
        this.typeRepository = typeRepository;
    }

    @Override
    public GraphQLObjectType getType(Object object) {
        List<GraphQLOutputType> candidates = typeRepository.getOutputTypes(object.getClass());
        if (candidates.size() == 1) {
            return (GraphQLObjectType) candidates.get(0);
        }
        return (GraphQLObjectType) typeRepository.getOutputType(((RelayNodeProxy) object).getGraphQLTypeHint());
    }
}
