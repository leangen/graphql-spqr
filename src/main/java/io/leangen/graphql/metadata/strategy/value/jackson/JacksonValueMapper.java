package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.InputFieldInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.InputParsingException;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JacksonValueMapper implements ValueMapper, InputFieldBuilder {

    private final ObjectMapper objectMapper;
    private final InputFieldInfoGenerator inputInfoGen = new InputFieldInfoGenerator();

    private static final Logger log = LoggerFactory.getLogger(JacksonValueMapper.class);

    JacksonValueMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T fromInput(Object graphQLInput, Type sourceType, AnnotatedType outputType) {
        try {
            return objectMapper.convertValue(graphQLInput, objectMapper.getTypeFactory().constructType(outputType.getType()));
        } catch (IllegalArgumentException e) {
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
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type.getType()));
        } catch (IOException e) {
            throw new InputParsingException(json, type.getType(), e);
        }
    }

    @Override
    public String toString(Object output) {
        if (output == null || output instanceof String) {
            return (String) output;
        }
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<InputField> getInputFields(InputFieldBuilderParams params) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(params.getType().getType());
        BeanDescription desc = objectMapper.getDeserializationConfig().introspect(javaType);
        return desc.findProperties().stream()
                .filter(BeanPropertyDefinition::couldDeserialize)
                .flatMap(prop -> toInputField(params.getType(), prop, params.getEnvironment()))
                .collect(Collectors.toSet());
    }

    private Stream<InputField> toInputField(AnnotatedType type, BeanPropertyDefinition prop, GlobalEnvironment environment) {
        ElementFactory elementFactory = new ElementFactory(type, environment.typeTransformer);
        return toInputField(elementFactory.fromProperty(prop), prop, objectMapper, environment);
    }

    private Stream<InputField> toInputField(TypedElement element, BeanPropertyDefinition prop, ObjectMapper objectMapper, GlobalEnvironment environment) {
        if (!environment.inclusionStrategy.includeInputField(prop.getMutator().getDeclaringClass(), element, element.getJavaType())) {
            return Stream.empty();
        }

        AnnotatedType deserializableType = resolveDeserializableType(prop.getMutator(), element.getJavaType(), prop.getPrimaryType(), objectMapper);
        Object defaultValue = inputInfoGen.defaultValue(element.getElements(), element.getJavaType(), environment).orElse(null);
        return Stream.of(new InputField(prop.getName(), prop.getMetadata().getDescription(), element, deserializableType, defaultValue));
    }

    private AnnotatedType resolveDeserializableType(Annotated accessor, AnnotatedType realType, JavaType baseType, ObjectMapper objectMapper) {
        AnnotationIntrospector introspector = objectMapper.getDeserializationConfig().getAnnotationIntrospector();
        try {
            objectMapper.getDeserializationContext().getFactory().mapAbstractType(objectMapper.getDeserializationConfig(), objectMapper.constructType(Map.class));
            JavaType refined = introspector.refineDeserializationType(objectMapper.getDeserializationConfig(), accessor, baseType);
            Class<?> raw = ClassUtils.getRawType(realType.getType());
            if (!refined.getRawClass().equals(raw)) {
                if (GenericTypeReflector.isSuperType(realType.getType(), refined.getRawClass())) {
                    AnnotatedType candidate = GenericTypeReflector.getExactSubType(realType, refined.getRawClass());
                    if (!ClassUtils.isMissingTypeParameters(candidate.getType())) {
                        return candidate;
                    }
                }
                return GenericTypeReflector.updateAnnotations(TypeUtils.toJavaType(refined), realType.getAnnotations());
            }
        } catch (JsonMappingException e) {
            /*no-op*/
        } catch (Exception e) {
            log.warn("Failed to determine the deserializable type for " + GenericTypeReflector.getTypeName(realType.getType())
                    + " due to an exception", e);
        }
        return realType;
    }

    protected Object defaultValue(AnnotatedType declaringType, BeanPropertyDefinition prop, AnnotatedType fieldType, TypeTransformer typeTransformer, GlobalEnvironment environment) {
        ElementFactory elementFactory = new ElementFactory(declaringType, typeTransformer);
        List<TypedElement> annotatedCandidates = Utils.flatten(
                Optional.ofNullable(prop.getConstructorParameter()).map(elementFactory::fromConstructorParameter),
                Optional.ofNullable(prop.getSetter()).map(elementFactory::fromSetter),
                Optional.ofNullable(prop.getGetter()).map(elementFactory::fromGetter),
                Optional.ofNullable(prop.getField()).map(elementFactory::fromField)
                ).collect(Collectors.toList());
        return inputInfoGen.defaultValue(annotatedCandidates, fieldType, environment).orElse(null);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }

    private static class ElementFactory {

        private final AnnotatedType type;
        private final TypeTransformer transformer;

        ElementFactory(AnnotatedType type, TypeTransformer typeTransformer) {
            this.type = type;
            this.transformer = typeTransformer;
        }

        TypedElement fromConstructorParameter(AnnotatedParameter ctorParam) {
            Executable constructor = (Executable) ctorParam.getOwner().getMember();
            Parameter parameter = constructor.getParameters()[ctorParam.getIndex()];
            AnnotatedType fieldType = transform(ClassUtils.getParameterTypes(constructor, type)[ctorParam.getIndex()], parameter);
            return new TypedElement(fieldType, parameter);
        }

        TypedElement fromSetter(AnnotatedMethod setterMethod) {
            Method setter = setterMethod.getAnnotated();
            AnnotatedType fieldType = transform(ClassUtils.getParameterTypes(setter, type)[0], setter, type);
            return new TypedElement(fieldType, setter);
        }

        TypedElement fromGetter(AnnotatedMethod getterMethod) {
            Method getter = getterMethod.getAnnotated();
            AnnotatedType fieldType = transform(ClassUtils.getReturnType(getter, type), getter, type);
            return new TypedElement(fieldType, getter);
        }

        TypedElement fromField(AnnotatedField fld) {
            Field field = fld.getAnnotated();
            AnnotatedType fieldType = transform(ClassUtils.getFieldType(field, type), field, type);
            return new TypedElement(fieldType, field);
        }

        TypedElement fromProperty(BeanPropertyDefinition prop) {
            Optional<TypedElement> constParam = Optional.ofNullable(prop.getConstructorParameter()).map(this::fromConstructorParameter);
            Optional<TypedElement> setter = Optional.ofNullable(prop.getSetter()).map(this::fromSetter);
            Optional<TypedElement> getter = Optional.ofNullable(prop.getGetter()).map(this::fromGetter);
            Optional<TypedElement> field = Optional.ofNullable(prop.getField()).map(this::fromField);

            Optional<TypedElement> mutator = Utils.flatten(constParam, setter, field).findFirst();
            Optional<TypedElement> explicit = Utils.flatten(constParam, setter, getter, field).filter(e -> e.isAnnotationPresent(GraphQLInputField.class)).findFirst();

            return new TypedElement(Utils.flatten(explicit, mutator, field).collect(Collectors.toList()));
        }

        AnnotatedType transform(AnnotatedType type, Member member, AnnotatedType declaringType) {
            try {
                return transformer.transform(type);
            } catch (TypeMappingException e) {
                throw new TypeMappingException(member, declaringType, e);
            }
        }

        AnnotatedType transform(AnnotatedType type, Parameter parameter) {
            try {
                return transformer.transform(type);
            } catch (TypeMappingException e) {
                throw new TypeMappingException(parameter.getDeclaringExecutable(), parameter, e);
            }
        }
    }

}
