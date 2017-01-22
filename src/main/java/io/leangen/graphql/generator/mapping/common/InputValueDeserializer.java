package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.generator.mapping.InputValueProvider;
import io.leangen.graphql.query.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InputValueDeserializer implements InputValueProvider {
    
    @Override
    public Object getInputValue(Object input, AnnotatedType type, ResolutionContext resolutionContext) {
        AnnotatedType argValueType = resolutionContext.getMappableType(type);
        return resolutionContext.valueMapper.fromInput(input, argValueType);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }
}
