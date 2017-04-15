package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class VoidToBooleanTypeAdapter extends AbstractTypeAdapter<Void, Boolean> {

    @Override
    public Boolean convertOutput(Void original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return true;
    }

    @Override
    public Void convertInput(Boolean substitute, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        throw new UnsupportedOperationException("Void used as input");
    }
}
