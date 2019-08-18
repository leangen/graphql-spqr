package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.annotations.types.GraphQLInterface;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class AnnotatedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

    @Override
    public boolean supportsInterface(AnnotatedType inter) {
        return inter.isAnnotationPresent(GraphQLInterface.class);
    }
}
