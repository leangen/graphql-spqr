package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.value.InputFieldInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnnotationIntrospector extends JacksonAnnotationIntrospector {

    private final MessageBundle messageBundle;
    private final Map<Type, List<NamedType>> typeMap;
    private final InputFieldInfoGenerator inputInfoGen;
    private final InclusionStrategy inclusionStrategy;
    private static final TypeResolverBuilder<?> typeResolverBuilder;

    static {
        JsonTypeInfo.Value settings = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY, ValueMapper.TYPE_METADATA_FIELD_NAME, null, false, null);
        typeResolverBuilder = new StdTypeResolverBuilder().init(settings, null);
    }

    AnnotationIntrospector(Map<Type, List<NamedType>> typeMap, InputFieldInfoGenerator inputInfoGen, InclusionStrategy inclusionStrategy, MessageBundle messageBundle) {
        this.typeMap = typeMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(typeMap);
        this.inputInfoGen = inputInfoGen;
        this.inclusionStrategy = inclusionStrategy;
        this.messageBundle = messageBundle;
    }

    @Override
    public PropertyName findNameForDeserialization(MapperConfig<?> config, Annotated annotated) {
        return inputInfoGen.getName(getAnnotatedCandidates(annotated), messageBundle)
                .map(PropertyName::construct)
                .orElse(super.findNameForDeserialization(config, annotated));
    }

    @Override
    public PropertyName findNameForSerialization(MapperConfig<?> config, Annotated annotated) {
        return inputInfoGen.getName(getAnnotatedCandidates(annotated), messageBundle)
                .map(PropertyName::construct)
                .orElse(super.findNameForSerialization(config, annotated));
    }

    @Override
    public String findPropertyDescription(MapperConfig<?> config, Annotated annotated) {
        return inputInfoGen.getDescription(getAnnotatedCandidates(annotated), messageBundle)
                .orElse(super.findPropertyDescription(config, annotated));
    }

    //@Override
    //public boolean hasIgnoreMarker(AnnotatedMember m) {
    //    return super.hasIgnoreMarker(m) || !inclusionStrategy.includeInputField(m.getAnnotated());
    //}

    @Override
    public Object findTypeResolverBuilder(MapperConfig<?> config, Annotated ann) {
        Object original = super.findTypeResolverBuilder(config, ann);
        if (original != null) {
            return original;
        }
        JsonTypeInfo.Value typeInfo = super.findPolymorphicTypeInfo(config, ann);
        if (typeInfo != null && typeInfo.getIdType() != JsonTypeInfo.Id.NONE) {
            return new StdTypeResolverBuilder(typeInfo);
        }
        if (typeMap.containsKey(ann.getRawType()) || (typeMap.isEmpty() && Utils.isNotEmpty(super.findSubtypes(config, ann)))) {
            return typeResolverBuilder;
        }
        return null;
    }

    @Override
    public List<NamedType> findSubtypes(MapperConfig<?> config, Annotated a) {
        List<NamedType> original = super.findSubtypes(config, a);
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
            return List.of(owner.getParameters()[parameter.getIndex()]);
        } else {
            List<AnnotatedElement> propertyMembers = ClassUtils.getPropertyMembers((Member & AnnotatedElement) annotated.getAnnotated());
            return !propertyMembers.isEmpty() ? propertyMembers : List.of(annotated.getAnnotated());
        }
    }
}
