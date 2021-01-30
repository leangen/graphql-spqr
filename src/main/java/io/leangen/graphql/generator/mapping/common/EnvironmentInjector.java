package io.leangen.graphql.generator.mapping.common;

import graphql.execution.MergedField;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Set;

public class EnvironmentInjector implements ArgumentInjector {
    
    private static final Type setOfStrings = new TypeToken<Set<String>>(){}.getType();
    
    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        Class<?> raw = GenericTypeReflector.erase(params.getType().getType());
        if (ResolutionEnvironment.class.isAssignableFrom(raw)) {
            return params.getResolutionEnvironment();
        }
        if (GenericTypeReflector.isSuperType(setOfStrings, params.getType().getType())) {
            return params.getResolutionEnvironment().dataFetchingEnvironment.getSelectionSet()
                    .getFieldsGroupedByResultKey().keySet();
        }
        if (MergedField.class.equals(raw)) {
            return params.getResolutionEnvironment().dataFetchingEnvironment.getMergedField();
        }
        if (ValueMapper.class.isAssignableFrom(raw)) {
            return params.getResolutionEnvironment().valueMapper;
        }
        throw new IllegalArgumentException("Argument of type " + raw.getName()
                + " can not be injected via @" + GraphQLEnvironment.class.getSimpleName());
    }

    @Override
    public boolean supports(AnnotatedType type, Parameter parameter) {
        return parameter != null && parameter.isAnnotationPresent(GraphQLEnvironment.class);
    }
}
