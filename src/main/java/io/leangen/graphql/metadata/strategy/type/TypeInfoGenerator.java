package io.leangen.graphql.metadata.strategy.type;

import io.leangen.graphql.generator.BuildContext;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface TypeInfoGenerator {

    String INPUT_SUFFIX = "Input";
    String SCALAR_SUFFIX = "Scalar";
    
    String generateTypeName(AnnotatedType type);

    String generateTypeDescription(AnnotatedType type);

    String[] getFieldOrder(AnnotatedType type);

    Set<Type> findAbstractTypes(AnnotatedType rootType, BuildContext buildContext);

    default String generateInputTypeName(AnnotatedType type) {
        return generateTypeName(type) + INPUT_SUFFIX;
    }

    default String generateInputTypeDescription(AnnotatedType type) {
        return generateTypeDescription(type);
    }

    default String generateScalarTypeName(AnnotatedType type) {
        return generateTypeName(type) + SCALAR_SUFFIX;
    }
    
    default String generateScalarTypeDescription(AnnotatedType type) {
        return generateTypeDescription(type);
    }
}
