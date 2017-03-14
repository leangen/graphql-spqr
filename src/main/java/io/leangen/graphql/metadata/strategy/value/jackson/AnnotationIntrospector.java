package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.util.Utils;

import static io.leangen.graphql.util.Utils.or;

public class AnnotationIntrospector extends JacksonAnnotationIntrospector {

    private static TypeResolverBuilder<?> typeResolverBuilder;
    private Map<Type, List<NamedType>> typeMap;
    
    static {
        typeResolverBuilder = new StdTypeResolverBuilder()
                .init(JsonTypeInfo.Id.NAME, null)
                .inclusion(JsonTypeInfo.As.PROPERTY);
    }

    public AnnotationIntrospector(Map<Type, List<NamedType>> typeMap) {
        this.typeMap = typeMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(typeMap);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated annotated) {
        return or(
                Optional.ofNullable(annotated.getAnnotation(GraphQLInputField.class))
                        .map(GraphQLInputField::name)
                        .filter(Utils::notEmpty),
                Optional.ofNullable(annotated.getAnnotation(GraphQLQuery.class))
                        .map(GraphQLQuery::name)
                        .filter(Utils::notEmpty))
                .map(PropertyName::new)
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
        return original == null ? typeResolverBuilder : original;
    }
    
    @Override
    public List<NamedType> findSubtypes(Annotated a) {
        List<NamedType> original = super.findSubtypes(a);
        if ((original != null && !original.isEmpty()) || !typeMap.containsKey(a.getRawType())) {
            return original;
        }
        return typeMap.get(a.getRawType());
    }
}
