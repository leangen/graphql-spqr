package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.execution.ResolutionContext;
import io.leangen.graphql.generator.mapping.ArgumentInjector;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InputValueDeserializer implements ArgumentInjector {
    
    @Override
    public Object getArgumentValue(Object input, AnnotatedType type, ResolutionContext resolutionContext) {
        AnnotatedType argValueType = resolutionContext.getMappableType(type);
        return resolutionContext.valueMapper.fromInput(input, argValueType);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }
}
