package io.leangen.graphql.metadata.strategy.value;

import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.metadata.DefaultValue;
import io.leangen.graphql.util.Defaults;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JsonDefaultValueProvider implements DefaultValueProvider {

    private static final AnnotatedType OBJECT = TypeFactory.annotatedClass(Object.class, new Annotation[0]);

    private final ValueMapper valueMapper;

    public JsonDefaultValueProvider() {
        this.valueMapper = Defaults.valueMapperFactory().getValueMapper();
    }

    @Override
    public DefaultValue getDefaultValue(AnnotatedElement targetElement, AnnotatedType type, DefaultValue initialValue) {
        if (initialValue.getValue() == null || initialValue.getValue().getClass() != String.class || type.getType() == String.class) {
            return initialValue;
        }
        return initialValue.map(v -> valueMapper.fromString((String) v, OBJECT));
    }
}
