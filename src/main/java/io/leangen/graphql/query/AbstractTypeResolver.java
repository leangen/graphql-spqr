package io.leangen.graphql.query;

import graphql.schema.TypeResolver;
import io.leangen.graphql.generator.TypeRepository;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class AbstractTypeResolver implements TypeResolver {

    protected TypeRepository typeRepository;

    public AbstractTypeResolver(TypeRepository typeRepository) {
        this.typeRepository = typeRepository;
    }
}
