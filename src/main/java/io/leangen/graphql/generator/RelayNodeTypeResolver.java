package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RelayNodeTypeResolver extends HintedTypeResolver {

    public RelayNodeTypeResolver(TypeRepository typeRepository, TypeInfoGenerator typeInfoGenerator) {
        super("node", typeRepository, typeInfoGenerator);
    }
}
