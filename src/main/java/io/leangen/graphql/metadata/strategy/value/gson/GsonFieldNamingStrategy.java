package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public String translateName(Field field) {
        return getNamedCandidates(field).stream()
                .map(this::getPropertyName)
                .reduce(Utils::or)
                .map(opt -> opt.orElse(fallback.translateName(field)))
                .get();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Optional<String> getPropertyName(AnnotatedElement annotated) {
        List<Optional<String>> explicitNames = new ArrayList<>();
        explicitNames.add(Optional.ofNullable(annotated.getAnnotation(GraphQLInputField.class))
                .map(GraphQLInputField::name));
        explicitNames.add(Optional.ofNullable(annotated.getAnnotation(GraphQLQuery.class))
                .map(GraphQLQuery::name));
        explicitNames.add(Optional.ofNullable(annotated.getAnnotation(SerializedName.class))
                .map(SerializedName::value));
        return explicitNames.stream()
                .map(opt -> opt.filter(Utils::notEmpty))
                .reduce(Utils::or).get();
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
