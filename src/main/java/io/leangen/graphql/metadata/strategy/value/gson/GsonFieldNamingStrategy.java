package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

public class GsonFieldNamingStrategy implements FieldNamingStrategy {

    private FieldNamingStrategy fallback;

    public GsonFieldNamingStrategy() {
        this(FieldNamingPolicy.IDENTITY);
    }

    public GsonFieldNamingStrategy(FieldNamingStrategy fallback) {
        this.fallback = fallback;
    }

    @Override
    public String translateName(Field field) {
        return getPropertyName(getNamedCandidates(field))
                .orElse(fallback.translateName(field));
    }

    private Optional<String> getPropertyName(List<AnnotatedElement> candidates) {
        Stream<Optional<String>> explicit = candidates.stream()
                .map(element -> Optional.ofNullable(element.getAnnotation(GraphQLInputField.class))
                        .map(GraphQLInputField::name));
        Stream<Optional<String>> queryImplicit = candidates.stream()
                .map(element -> Optional.ofNullable(element.getAnnotation(GraphQLQuery.class))
                        .map(GraphQLQuery::name));
        Stream<Optional<String>> gsonExplicit = candidates.stream()
                .map(element -> Optional.ofNullable(element.getAnnotation(SerializedName.class))
                        .map(SerializedName::value));
        return Utils.concat(explicit, queryImplicit, gsonExplicit)
                .map(opt -> opt.filter(Utils::notEmpty))
                .reduce(Utils::or).orElse(Optional.empty());
    }

    private List<AnnotatedElement> getNamedCandidates(Field field) {
        List<AnnotatedElement> propertyElements = new ArrayList<>(3);
        try {
            propertyElements.add(ClassUtils.findSetter(field.getDeclaringClass(), field.getName(), field.getType()));
        } catch (NoSuchMethodException e) {/*no-op*/}
        try {
            propertyElements.add(ClassUtils.findGetter(field.getDeclaringClass(), field.getName()));
        } catch (NoSuchMethodException e) {/*no-op*/}
        propertyElements.add(field);
        return propertyElements;
    }
}
