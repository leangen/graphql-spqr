package io.leangen.graphql.metadata.strategy.type;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface DomainTypeNameGenerator {
    
    String generateTypeName(AnnotatedType type);
    
    String generateInputTypeName(AnnotatedType type);
}
