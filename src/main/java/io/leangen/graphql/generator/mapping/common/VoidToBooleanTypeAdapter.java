package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.query.ExecutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class VoidToBooleanTypeAdapter extends AbstractTypeAdapter<Void, Boolean> {

    @Override
    public Boolean convertOutput(Void original, AnnotatedType type, ExecutionContext executionContext) {
        return true;
    }

    @Override
    public Void convertInput(Boolean substitute, AnnotatedType type, ExecutionContext executionContext) {
        throw new UnsupportedOperationException("Void used as input");
    }
}
