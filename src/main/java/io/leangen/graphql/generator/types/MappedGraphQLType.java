package io.leangen.graphql.generator.types;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface MappedGraphQLType {

    AnnotatedType getJavaType();
}
