package io.leangen.graphql.generator.mapping.common;

import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class VoidToBooleanTypeAdapter extends AbstractTypeAdapter<Void, Boolean> {

    @Override
    public Boolean convertOutput(Void original) {
        return true;
    }

    @Override
    public Void convertInput(Boolean substitute) {
        throw new UnsupportedOperationException("Void used as input");
    }
}
