package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface InterfaceMappingStrategy {

    Collection<AnnotatedType> getInterfaces(AnnotatedType type);

    boolean supports(AnnotatedType interfase);
}
