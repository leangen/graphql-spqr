package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import graphql.language.Field;
import graphql.schema.GraphQLObjectType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

public class EnvironmentInjector implements ArgumentInjector {
    
    private static final Type listOfFields = new TypeToken<List<Field>>(){}.getType();
    private static final Type setOfStrings = new TypeToken<Set<String>>(){}.getType();
    
    @Override
    public Object getArgumentValue(Object rawInput, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        if (GenericTypeReflector.isSuperType(setOfStrings, type.getType()) && resolutionEnvironment.fieldType instanceof GraphQLObjectType) {
            return Collections.<String>emptySet();
        }
        Class raw = GenericTypeReflector.erase(type.getType());
        if (Field.class.equals(raw)) {
            return resolutionEnvironment.fields.get(0);
        }
        if (GenericTypeReflector.isSuperType(listOfFields, type.getType())) {
            return resolutionEnvironment.fields;
        }
        if (ValueMapper.class.isAssignableFrom(raw)) {
            return resolutionEnvironment.valueMapper;
        }
        if (ResolutionEnvironment.class.isAssignableFrom(raw)) {
            return resolutionEnvironment;
        }
        throw new IllegalArgumentException("Argument of type " + raw.getName() 
                + " can not be injected via @" + EnvironmentInjector.class.getSimpleName());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLEnvironment.class);
    }
}
