package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.value.InputFieldInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnnotationIntrospector extends JacksonAnnotationIntrospector {

    private final MessageBundle messageBundle;
    private final Map<Type, List<NamedType>> typeMap;
    private final InputFieldInfoGenerator inputInfoGen = new InputFieldInfoGenerator();
    private static final TypeResolverBuilder<?> typeResolverBuilder;

    static {
        typeResolverBuilder = new StdTypeResolverBuilder()
                .init(JsonTypeInfo.Id.NAME, null)
                .inclusion(JsonTypeInfo.As.PROPERTY)
                .typeProperty(ValueMapper.TYPE_METADATA_FIELD_NAME);
    }

    AnnotationIntrospector(Map<Type, List<NamedType>> typeMap, MessageBundle messageBundle) {
        this.typeMap = typeMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(typeMap);
        this.messageBundle = messageBundle;
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated annotated) {
        return inputInfoGen.getName(getAnnotatedCandidates(annotated), messageBundle)
                .map(PropertyName::construct)
                .orElse(super.findNameForDeserialization(annotated));
    }

    @Override
    public PropertyName findNameForSerialization(Annotated annotated) {
        return inputInfoGen.getName(getAnnotatedCandidates(annotated), messageBundle)
                .map(PropertyName::construct)
                .orElse(super.findNameForSerialization(annotated));
    }

    @Override
    public String findPropertyDescription(Annotated annotated) {
        return inputInfoGen.getDescription(getAnnotatedCandidates(annotated), messageBundle)
                .orElse(super.findPropertyDescription(annotated));
    }

    /**
     * Provides a {@link TypeResolverBuilder} configured the same way as if the given {@link AnnotatedClass}
     * was annotated with {@code @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY}
     *
     * @implNote Only provides a {@link TypeResolverBuilder} if Jackson can't already construct one,
     * this way if Jackson annotations are used (e.g. {@link JsonTypeInfo}) they will still be respected.
     * {@inheritDoc}
     */
    @Override
    public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config, AnnotatedClass ac, JavaType baseType) {
        TypeResolverBuilder<?> original = super.findTypeResolver(config, ac, baseType);
        if (original != null) {
            return original;
        }
        if (typeMap.containsKey(ac.getRawType()) || (typeMap.isEmpty() && Utils.isNotEmpty(super.findSubtypes(ac)))) {
            return typeResolverBuilder;
        }
        return null;
    }

    @Override
    public List<NamedType> findSubtypes(Annotated a) {
        List<NamedType> original = super.findSubtypes(a);
        if ((original == null || original.isEmpty()) && typeMap.containsKey(a.getRawType())) {
            return typeMap.get(a.getRawType());
        }
        // Don't return the original here as this AnnotationIntrospector is one of a pair,
        // and if both return results, they'll end up merged and duplicated
        return null;
    }

    @Override
    public String[] findEnumValues(MapperConfig<?> config, AnnotatedClass annotatedClass,
                                   Enum<?>[] enumValues, String[] names) {
        String[] jacksonNames = super.findEnumValues(config, annotatedClass, enumValues, names);
        for (int i = 0; i < enumValues.length; i++) {
            GraphQLEnumValue annotation = ClassUtils.getEnumConstantField(enumValues[i]).getAnnotation(GraphQLEnumValue.class);
            if (annotation != null && Utils.isNotEmpty(annotation.name())) {
                jacksonNames[i] = messageBundle.interpolate(annotation.name());
            }
        }
        return jacksonNames;
    }

    private List<AnnotatedElement> getAnnotatedCandidates(Annotated annotated) {
        if (annotated instanceof AnnotatedParameter) {
            AnnotatedParameter parameter = (AnnotatedParameter) annotated;
            Executable owner = (Executable) parameter.getOwner().getAnnotated();
            return Collections.singletonList(owner.getParameters()[parameter.getIndex()]);
        }
        if (annotated instanceof AnnotatedField) {
            Field field = ((AnnotatedField) annotated).getAnnotated();
            return ClassUtils.getPropertyMembers(field);
        }
        if (annotated instanceof AnnotatedMethod) {
            Method setter = ((AnnotatedMethod) annotated).getAnnotated();
            return ClassUtils.getPropertyMembers(setter);
        }
        return Collections.emptyList();
    }
}
