package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

/**
 * @author Sven Barden
 */
public interface GroupAnnotationSupport {

    boolean support(AnnotatedType type);
}
