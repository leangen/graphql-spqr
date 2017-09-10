package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.annotations.types.GraphQLInterface;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class AnnotatedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

    public AnnotatedInterfaceStrategy(boolean mapClasses) {
        super(mapClasses);
    }

    @Override
    public boolean supportsInterface(AnnotatedType interfase) {
        return interfase.isAnnotationPresent(GraphQLInterface.class);
    }
}
