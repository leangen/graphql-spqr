package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.InputFieldDiscoveryStrategy;
import io.leangen.graphql.metadata.strategy.value.InputFieldInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.InputParsingException;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class GsonValueMapper implements ValueMapper, InputFieldDiscoveryStrategy {

    private final Gson gson;
    private final InputFieldInfoGenerator inputInfoGen = new InputFieldInfoGenerator();

    public GsonValueMapper(Gson gson) {
        this.gson = gson;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromInput(Object graphQLInput, Type sourceType, AnnotatedType outputType) throws InputParsingException {
        if (graphQLInput.getClass() == outputType.getType()) {
            return (T) graphQLInput;
        }
        try {
            JsonElement jsonElement = gson.toJsonTree(graphQLInput, sourceType);
            return gson.fromJson(jsonElement, outputType.getType());
        } catch (JsonSyntaxException e) {
            throw new InputParsingException(graphQLInput, outputType.getType(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(String json, AnnotatedType type) {
        if (json == null || String.class.equals(type.getType())) {
            return (T) json;
        }
        try {
            return gson.fromJson(json, type.getType());
        } catch (JsonSyntaxException e) {
            throw new InputParsingException(json, type.getType(), e);
        }
    }

    @Override
    public String toString(Object output) {
        if (output == null || output instanceof String) {
            return (String) output;
        }
        return gson.toJson(output);
    }

    /**
     * Unlike Jackson, Gson doesn't expose any of its metadata, so this method is more or less a
     * reimplementation of {@link com.google.gson.internal.bind.ReflectiveTypeAdapterFactory#getBoundFields(Gson, com.google.gson.reflect.TypeToken, Class)}
     *
     * @param type Java type (used as query input) to be analyzed for deserializable fields
     * @param inclusionStrategy The strategy that decides which input fields are acceptable
     * @param typeTransformer Transformer used to pre-process the types (can be used to complete the missing generics etc)
     *
     * @return All deserializable fields that could be discovered from this {@link AnnotatedType}
     */
    @Override
    public Set<InputField> getInputFields(AnnotatedType type, InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer) {
        Set<InputField> inputFields = new HashSet<>();
        Class<?> raw = ClassUtils.getRawType(type.getType());
        if (raw.isInterface() || raw.isPrimitive()) {
            return inputFields;
        }

        while (raw != Object.class) {
            Field[] fields = raw.getDeclaredFields();
            for (Field field : fields) {
                if (gson.excluder().excludeClass(field.getType(), false)
                        || gson.excluder().excludeField(field, false)) {
                    continue;
                }
                AnnotatedType fieldType;
                try {
                    fieldType = typeTransformer.transform(ClassUtils.getFieldType(field, type));
                } catch (TypeMappingException e) {
                    throw new TypeMappingException(field, type, e);
                }
                Optional<Method> setter = ClassUtils.findSetter(field.getDeclaringClass(), field.getName(), field.getType());
                Member target = setter.isPresent() ? setter.get() : field;
                if (!inclusionStrategy.includeInputField(target.getDeclaringClass(), (AnnotatedElement) target, fieldType)) {
                    continue;
                }
                field.setAccessible(true);
                String fieldName = gson.fieldNamingStrategy().translateName(field);
                if (!inputFields.add(new InputField(fieldName, getDescription(field), fieldType, null, defaultValue(field, fieldType)))) {
                    throw new IllegalArgumentException(raw + " declares multiple input fields named " + fieldName);
                }
            }
            raw = raw.getSuperclass();
            type = GenericTypeReflector.getExactSuperType(type, raw);
        }
        return inputFields;
    }

    protected String getDescription(Field field) {
        return inputInfoGen.getDescription(ClassUtils.getPropertyMembers(field)).orElse(null);
    }

    protected Object defaultValue(Field field, AnnotatedType fieldType) {
        return inputInfoGen.defaultValue(ClassUtils.getPropertyMembers(field), fieldType).orElse(null);
    }
}
