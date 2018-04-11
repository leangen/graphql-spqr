package io.leangen.graphql.generator.mapping.common;

import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InputValueDeserializer implements ArgumentInjector {
    
    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        if (params.getInput() == null) {
            return null;
        }
        return params.getResolutionEnvironment().valueMapper.fromInput(params.getInput(), params.getType());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }
}
