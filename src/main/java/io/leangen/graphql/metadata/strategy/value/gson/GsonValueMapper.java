package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.InputFieldInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.InputParsingException;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GsonValueMapper implements ValueMapper, InputFieldBuilder {

    private final Gson gson;
    private final InputFieldInfoGenerator inputInfoGen = new InputFieldInfoGenerator();
    private static final Gson NO_CONVERTERS = new Gson();

    GsonValueMapper(Gson gson) {
        this.gson = gson;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromInput(Object graphQLInput, Type sourceType, AnnotatedType outputType) throws InputParsingException {
        if (graphQLInput.getClass() == outputType.getType()) {
            return (T) graphQLInput;
        }
        try {
            JsonElement jsonElement = NO_CONVERTERS.toJsonTree(graphQLInput, sourceType);
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
    public String toString(Object output, AnnotatedType type) {
        if (output == null || output instanceof String) {
            return (String) output;
        }
        return gson.toJson(output, type.getType());
    }

    /**
     * Unlike Jackson, Gson doesn't expose any of its metadata, so this method is more or less a
     * reimplementation of {@link com.google.gson.internal.bind.ReflectiveTypeAdapterFactory#getBoundFields(Gson, com.google.gson.reflect.TypeToken, Class)}
     *
     * @param params The parameters available to the discovery strategy
     *
     * @return All deserializable fields that could be discovered from this {@link AnnotatedType}
     */
    @Override
    @SuppressWarnings("JavadocReference")
    public Set<InputField> getInputFields(InputFieldBuilderParams params) {
        Set<InputField> inputFields = new HashSet<>();
        AnnotatedType type = params.getType();
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
                TypedElement element = reduce(type, field, params.getEnvironment().typeTransformer);
                if (!params.getEnvironment().inclusionStrategy.includeInputField(field.getDeclaringClass(), element, element.getJavaType())) {
                    continue;
                }
                field.setAccessible(true);
                String fieldName = gson.fieldNamingStrategy().translateName(field);
                InputField inputField = new InputField(fieldName, getDescription(field, params.getEnvironment().messageBundle),
                        element, null, defaultValue(field, element.getJavaType(), params.getEnvironment()));
                if (!inputFields.add(inputField)) {
                    throw new IllegalArgumentException(raw + " declares multiple input fields named " + fieldName);
                }
            }
            raw = raw.getSuperclass();
            type = GenericTypeReflector.getExactSuperType(type, raw);
        }
        return inputFields;
    }

    @SuppressWarnings("WeakerAccess")
    protected TypedElement reduce(AnnotatedType declaringType, Field field, TypeTransformer transformer) {
        Optional<TypedElement> fld = Optional.of(element(ClassUtils.getFieldType(field, declaringType), field, declaringType, transformer));
        Optional<TypedElement> setter = ClassUtils.findSetter(field.getDeclaringClass(), field.getName(), field.getType())
                .map(mutator -> element(ClassUtils.getParameterTypes(mutator, declaringType)[0], mutator, declaringType, transformer));
        Optional<TypedElement> getter = ClassUtils.findGetter(field.getDeclaringClass(), field.getName())
                .filter(accessor -> accessor.isAnnotationPresent(GraphQLInputField.class))
                .map(accessor -> element(ClassUtils.getReturnType(accessor, declaringType), accessor, declaringType, transformer));
        return new TypedElement(Utils.flatten(getter, setter, fld).collect(Collectors.toList()));
    }

    protected String getDescription(Field field, MessageBundle messageBundle) {
        return inputInfoGen.getDescription(ClassUtils.getPropertyMembers(field), messageBundle).orElse(null);
    }

    protected Object defaultValue(Field field, AnnotatedType fieldType, GlobalEnvironment environment) {
        return inputInfoGen.defaultValue(ClassUtils.getPropertyMembers(field), fieldType, environment).orElse(null);
    }

    private <T extends Member & AnnotatedElement> TypedElement element(AnnotatedType type, T annotatedMember, AnnotatedType declaringType, TypeTransformer transformer) {
        try {
            return new TypedElement(transformer.transform(type), annotatedMember);
        } catch (TypeMappingException e) {
            throw new TypeMappingException(annotatedMember, declaringType, e);
        }
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }
}
