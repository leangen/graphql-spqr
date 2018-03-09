package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.leangen.graphql.util.Utils.or;

public class AnnotationIntrospector extends JacksonAnnotationIntrospector {

    private static TypeResolverBuilder<?> typeResolverBuilder;
    private Map<Type, List<NamedType>> typeMap;

    private static final Logger log = LoggerFactory.getLogger(AnnotationIntrospector.class);

    static {
        typeResolverBuilder = new StdTypeResolverBuilder()
                .init(JsonTypeInfo.Id.NAME, null)
                .inclusion(JsonTypeInfo.As.PROPERTY)
                .typeProperty(ValueMapper.TYPE_METADATA_FIELD_NAME);
    }

    AnnotationIntrospector(Map<Type, List<NamedType>> typeMap) {
        this.typeMap = typeMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(typeMap);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated annotated) {
        List<AnnotatedElement> namedCandidates = getNamedCandidates(annotated);
        return Stream.concat(
                namedCandidates.stream()
                        .map(member -> Optional.ofNullable(member.getAnnotation(GraphQLInputField.class))
                                .map(GraphQLInputField::name)),
                namedCandidates.stream()
                        .map(member -> Optional.ofNullable(member.getAnnotation(GraphQLQuery.class))
                                .map(GraphQLQuery::name)))
                .map(opt -> opt.filter(Utils::notEmpty))
                .reduce(Utils::or)
                .flatMap(optName -> optName.map(PropertyName::new))
                .orElse(super.findNameForDeserialization(annotated));
    }

    @Override
    public String findPropertyDescription(Annotated annotated) {
        return or(
                Optional.ofNullable(annotated.getAnnotation(GraphQLInputField.class))
                        .map(GraphQLInputField::description)
                        .filter(Utils::notEmpty),
                Optional.ofNullable(annotated.getAnnotation(GraphQLQuery.class))
                        .map(GraphQLQuery::description)
                        .filter(Utils::notEmpty))
                .orElse(super.findPropertyDescription(annotated));
    }

    /**
     * Provides a {@link TypeResolverBuilder} configured the same way as if the given {@link AnnotatedClass}
     * was annotated with {@code @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY}
     *
     * @implNote Only provides a {@link TypeResolverBuilder} if Jackson can't already construct one,
     * this way if Jackson annotations are used (e.g. {@link JsonTypeInfo}) they will still be respected.
     *
     * {@inheritDoc}
     */
    @Override
    public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config, AnnotatedClass ac, JavaType baseType) {
        TypeResolverBuilder<?> original = super.findTypeResolver(config, ac, baseType);
        return original == null && typeMap.containsKey(ac.getRawType()) ? typeResolverBuilder : original;
    }

    @Override
    public List<NamedType> findSubtypes(Annotated a) {
        List<NamedType> original = super.findSubtypes(a);
        if ((original == null || original.isEmpty()) && typeMap.containsKey(a.getRawType())) {
            return typeMap.get(a.getRawType());
        }
        return original;
    }

    @Override
    public String[] findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] defaultNames) {
        String[] jacksonNames = super.findEnumValues(enumType, enumValues, defaultNames);
        for (int i = 0; i < enumValues.length; i++) {
            GraphQLEnumValue annotation = ClassUtils.getEnumConstantField(enumValues[i]).getAnnotation(GraphQLEnumValue.class);
            if (annotation != null && Utils.notEmpty(annotation.name())) {
                jacksonNames[i] = annotation.name();
            }
        }
        return jacksonNames;
    }

    private List<AnnotatedElement> getNamedCandidates(Annotated annotated) {
        List<AnnotatedElement> propertyElements = new ArrayList<>(3);
        if (annotated instanceof AnnotatedField) {
            AnnotatedField field = ((AnnotatedField) annotated);
            try {
                Arrays.stream(Introspector.getBeanInfo(field.getDeclaringClass()).getPropertyDescriptors())
                        .filter(prop -> field.getName().equals(prop.getName()))
                        .findFirst()
                        .ifPresent(prop -> addPropertyMethods(propertyElements, prop));
            } catch (IntrospectionException e) {
                log.warn("Introspection of {} failed. GraphQL input fields might be incorrectly mapped.",
                        field.getDeclaringClass());
            }
            propertyElements.add(annotated.getAnnotated());
        } else if (annotated instanceof AnnotatedMethod) {
            Method setter = (Method) annotated.getAnnotated();
            try {
                Arrays.stream(Introspector.getBeanInfo(setter.getDeclaringClass()).getPropertyDescriptors())
                        .filter(prop -> setter.equals(prop.getWriteMethod()))
                        .findFirst()
                        .ifPresent(prop -> addPropertyMethods(propertyElements, prop));
            } catch (IntrospectionException e) {
                log.warn("Introspection of {} failed. GraphQL input fields might be incorrectly mapped.",
                        setter.getDeclaringClass());
            }
            if (propertyElements.isEmpty() && ClassUtils.isGetter(setter)) {
                propertyElements.add(setter);
            }
        }
        return propertyElements;
    }

    private void addPropertyMethods(List<AnnotatedElement> propertyElements, PropertyDescriptor prop) {
        if (prop.getWriteMethod() != null) {
            propertyElements.add(prop.getWriteMethod());
        }
        if (prop.getReadMethod() != null) {
            propertyElements.add(prop.getReadMethod());
        }
    }
}
