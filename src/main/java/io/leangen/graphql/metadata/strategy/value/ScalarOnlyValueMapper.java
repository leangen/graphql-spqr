package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

public class ScalarOnlyValueMapper implements ValueMapper {

    private static final SimpleStringValueMapper STRING_MAPPER = new SimpleStringValueMapper();
    private static final String MAPPING_ERROR = "Simple value mapping failed. " +
            "For complex object support, add Gson or Jackson to classpath, or implement a custom ValueMapperFactory.";
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromInput(Object graphQlInput, Type sourceType, AnnotatedType type) {
        if (graphQlInput.getClass() == type.getType()) {
            return (T) graphQlInput;
        }
        throw new IllegalArgumentException(MAPPING_ERROR);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(String json, AnnotatedType type) {
        if (STRING_MAPPER.supports(type.getType())) {
            return STRING_MAPPER.deserialize(json, type.getType());
        }
        throw new IllegalArgumentException(MAPPING_ERROR);
    }

    @Override
    public String toString(Object output) {
        if (STRING_MAPPER.supports(output.getClass())) {
            return STRING_MAPPER.serialize(output);
        }
        throw new IllegalArgumentException(MAPPING_ERROR);
    }
}
