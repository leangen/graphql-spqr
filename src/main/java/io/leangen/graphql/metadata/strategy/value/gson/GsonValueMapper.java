package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.DefaultValue;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.TypeDiscriminatorField;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.InputFieldInclusionParams;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.*;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Scalars;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static io.leangen.geantyref.GenericTypeReflector.isBoxType;
import static io.leangen.graphql.util.ClassUtils.isPrimitive;

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
            if (Scalars.isScalar(type.getType()) && !isPrimitive(type) && !isBoxType(type.getType())) {
                return (T) Scalars.toGraphQLScalarType(type.getType()).getCoercing().parseValue(json);
            }
            return gson.fromJson(json, type.getType());
        } catch (JsonSyntaxException e) {
            throw new InputParsingException(json, type.getType(), e);
        }
    }

    @Override
    public String toString(Object output, AnnotatedType type) {
        if (output != null && Scalars.isScalar(type.getType())) {
            output = Scalars.toGraphQLScalarType(type.getType()).getCoercing().serialize(output);
        }
        if (output == null || output instanceof String) {
            return (String) output;
        }
        return gson.toJson(output, type.getType());
    }

    /**
     * Unlike Jackson, Gson doesn't expose any of its metadata, so this method is more or less a
     * reimplementation of {@code com.google.gson.internal.bind.ReflectiveTypeAdapterFactory#getBoundFields(Gson, com.google.gson.reflect.TypeToken, Class)}
     *
     * @param params The parameters available to the discovery strategy
     *
     * @return All deserializable fields that could be discovered from this {@link AnnotatedType}
     */
    @Override
    public Set<InputField> getInputFields(InputFieldBuilderParams params) {
        Map<String, InputField> explicit = fromFields(params);
        Map<String, InputField> implicit = fromGetters(params);
        Map<String, InputField> merged = new HashMap<>(explicit);
        implicit.forEach(merged::putIfAbsent);
        return new HashSet<>(merged.values());
    }

    @SuppressWarnings("deprecation")
    private Map<String, InputField> fromFields(InputFieldBuilderParams params) {
        AnnotatedType type = params.getType();
        Class<?> raw = ClassUtils.getRawType(type.getType());
        if (raw.isInterface() || raw.isPrimitive()) {
            return Collections.emptyMap();
        }

        Map<String, InputField> inputFields = new HashMap<>();
        while (raw != Object.class) {
            Field[] fields = raw.getDeclaredFields();
            for (Field field : fields) {
                if (gson.excluder().excludeClass(field.getType(), false)
                        || gson.excluder().excludeField(field, false)) {
                    continue;
                }
                List<AnnotatedElement> propertyMembers = ClassUtils.getPropertyMembers(field);
                String fieldName = gson.fieldNamingStrategy().translateName(field);
                InputFieldInclusionParams inclusionParams = InputFieldInclusionParams.builder()
                        .withType(params.getType())
                        .withElementDeclaringClass(field.getDeclaringClass())
                        .withElements(propertyMembers)
                        .withDeserializationInfo(true, isDeserializableInSubType(fieldName, gson.fieldNamingStrategy(), params.getConcreteSubTypes()))
                        .build();
                if (!params.getEnvironment().inclusionStrategy.includeInputField(inclusionParams)) {
                    continue;
                }
                TypedElement element = reduce(type, field, params.getEnvironment().typeTransformer);
                field.setAccessible(true);
                InputField inputField = new InputField(fieldName, getDescription(propertyMembers, params.getEnvironment().messageBundle),
                        element, null, defaultValue(propertyMembers, element.getJavaType(), params.getEnvironment()));
                if (inputFields.containsKey(fieldName)) {
                    throw new IllegalArgumentException(raw + " declares multiple input fields named " + fieldName);
                }
                inputFields.put(fieldName, inputField);
            }
            raw = raw.getSuperclass();
            type = GenericTypeReflector.getExactSuperType(type, raw);
        }
        return inputFields;
    }

    private Map<String, InputField> fromGetters(InputFieldBuilderParams params) {
        AnnotatedType type = params.getType();
        Class<?> raw = ClassUtils.getRawType(type.getType());

        GsonFieldNamingStrategy namingStrategy = new GsonFieldNamingStrategy(params.getEnvironment().messageBundle);
        List<Method> getters = Arrays.stream(raw.getMethods())
                .filter(ClassUtils::isGetter)
                .collect(Collectors.toList());
        Map<String, InputField> inputFields = new HashMap<>();
        for (Method getter : getters) {
            if (gson.excluder().excludeClass(getter.getReturnType(), false)) {
                continue;
            }
            List<AnnotatedElement> propertyMembers = ClassUtils.getPropertyMembers(getter);
            if (propertyMembers.stream().anyMatch(element -> element instanceof Field)) {
                continue;
            }
            String fieldName = namingStrategy.getPropertyName(propertyMembers)
                    .orElse(ClassUtils.getFieldNameFromGetter(getter));
            InputFieldInclusionParams inclusionParams = InputFieldInclusionParams.builder()
                    .withType(params.getType())
                    .withElementDeclaringClass(getter.getDeclaringClass())
                    .withElements(propertyMembers)
                    .withDeserializationInfo(false, isDeserializableInSubType(fieldName, gson.fieldNamingStrategy(), params.getConcreteSubTypes()))
                    .build();
            if (!params.getEnvironment().inclusionStrategy.includeInputField(inclusionParams)) {
                continue;
            }
            TypedElement element = reduce(type, getter, params.getEnvironment().typeTransformer);
            InputField inputField = new InputField(fieldName, getDescription(propertyMembers, params.getEnvironment().messageBundle),
                    element, null, defaultValue(propertyMembers, element.getJavaType(), params.getEnvironment()));
            if (inputFields.containsKey(fieldName)) {
                throw new IllegalArgumentException(raw + " declares multiple input fields named " + fieldName);
            }
            inputFields.put(fieldName, inputField);
        }
        return inputFields;
    }

    @Override
    public TypeDiscriminatorField getTypeDiscriminatorField(InputFieldBuilderParams params) {
        String[] subTypes = params.getConcreteSubTypes().stream()
                .map(GenericTypeReflector::annotate)
                .map(impl -> params.getEnvironment().typeInfoGenerator.generateTypeName(impl, params.getEnvironment().messageBundle))
                .toArray(String[]::new);

        if (subTypes.length > 1) {
            return new TypeDiscriminatorField(ValueMapper.TYPE_METADATA_FIELD_NAME, "Input type discriminator", subTypes);
        }
        return null;
    }

    protected TypedElement reduce(AnnotatedType declaringType, Field field, TypeTransformer transformer) {
        Optional<TypedElement> fld = Optional.of(element(ClassUtils.getFieldType(field, declaringType), field, declaringType, transformer));
        Optional<TypedElement> setter = ClassUtils.findSetter(field)
                .map(mutator -> element(ClassUtils.getParameterTypes(mutator, declaringType)[0], mutator, declaringType, transformer));
        Optional<TypedElement> getter = ClassUtils.findGetter(field)
                .filter(accessor -> accessor.isAnnotationPresent(GraphQLInputField.class))
                .map(accessor -> element(ClassUtils.getReturnType(accessor, declaringType), accessor, declaringType, transformer));
        return new TypedElement(Utils.flatten(getter, setter, fld).collect(Collectors.toList()));
    }

    protected TypedElement reduce(AnnotatedType declaringType, Method getter, TypeTransformer transformer) {
        Optional<TypedElement> setter = ClassUtils.findSetter(getter)
                .map(mutator -> element(ClassUtils.getParameterTypes(mutator, declaringType)[0], mutator, declaringType, transformer));
        Optional<TypedElement> gtr = Optional.of(getter)
                .map(accessor -> element(ClassUtils.getReturnType(accessor, declaringType), accessor, declaringType, transformer));
        List<TypedElement> elements = Utils.flatten(gtr, setter).collect(Collectors.toList());
        return new TypedElement(elements);
    }

    protected String getDescription(List<AnnotatedElement> members, MessageBundle messageBundle) {
        return inputInfoGen.getDescription(members, messageBundle).orElse(null);
    }

    protected DefaultValue defaultValue(List<AnnotatedElement> members, AnnotatedType fieldType, GlobalEnvironment environment) {
        return inputInfoGen.defaultValue(members, fieldType, environment);
    }

    private <T extends Member & AnnotatedElement> TypedElement element(AnnotatedType type, T annotatedMember, AnnotatedType declaringType, TypeTransformer transformer) {
        try {
            return new TypedElement(transformer.transform(type), annotatedMember);
        } catch (TypeMappingException e) {
            throw TypeMappingException.ambiguousMemberType(annotatedMember, declaringType, e);
        }
    }

    private boolean isDeserializableInSubType(String fieldName, FieldNamingStrategy namingStrategy, List<Class<?>> concreteSubTypes) {
        return concreteSubTypes.stream().anyMatch(impl -> ClassUtils.findField(impl, field -> namingStrategy.translateName(field).equals(fieldName)).isPresent());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }
}
