package io.leangen.graphql.query;

import graphql.schema.GraphQLObjectType;
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
//        Selection selection = executionContext.getOperationDefinition().getSelectionSet().getSelections().get(0);
//        if (selection instanceof Field) {
//            String gQlType = (String) executionContext.getFieldMeta().get(selection);
//            return (GraphQLObjectType) executionContext.getGraphQLSchema().getType(gQlType);
//        }
        return (GraphQLObjectType) typeRepository.getOutputType(((RelayNodeProxy) object).getGraphQLTypeHint());
    }
}
