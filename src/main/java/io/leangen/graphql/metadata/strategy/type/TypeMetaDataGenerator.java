package io.leangen.graphql.metadata.strategy.type;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface TypeMetaDataGenerator {

    String INPUT_SUFFIX = "_input";
    String SCALAR_SUFFIX = "_scalar";
    
    String generateTypeName(AnnotatedType type);

    String generateTypeDescription(AnnotatedType type);
    
    default String generateInputTypeName(AnnotatedType type) {
        return generateTypeName(type) + INPUT_SUFFIX;
    }
    
    default String generateScalarTypeName(AnnotatedType type) {
        return generateTypeName(type) + SCALAR_SUFFIX;
    }
    
    default String generateInputTypeDescription(AnnotatedType type) {
        return generateTypeDescription(type);
    }
    
    default String generateScalarTypeDescription(AnnotatedType type) {
        return generateTypeDescription(type);
    }
}
