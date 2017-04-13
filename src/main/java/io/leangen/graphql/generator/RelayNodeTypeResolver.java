package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.strategy.type.TypeMetaDataGenerator;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RelayNodeTypeResolver extends HintedTypeResolver {

    public RelayNodeTypeResolver(TypeRepository typeRepository, TypeMetaDataGenerator typeMetaDataGenerator) {
        super("node", typeRepository, typeMetaDataGenerator);
    }
}
