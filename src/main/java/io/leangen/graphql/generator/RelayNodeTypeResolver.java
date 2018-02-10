package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.util.GraphQLUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
class RelayNodeTypeResolver extends DelegatingTypeResolver {

    RelayNodeTypeResolver(TypeRepository typeRepository, TypeInfoGenerator typeInfoGenerator) {
        super(GraphQLUtils.NODE, typeRepository, typeInfoGenerator);
    }
}
