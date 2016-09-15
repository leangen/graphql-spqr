package io.leangen.graphql.query;

import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.generator.proxy.RelayNodeProxy;

import java.util.List;

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
        List<GraphQLObjectType> candidates = typeRepository.getOutputTypes(object.getClass());
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return typeRepository.getOutputType(((RelayNodeProxy) object).getGraphQLTypeHint());
    }
}
