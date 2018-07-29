package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ReservedStrings;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Optional;

public class InputFieldInfoGenerator {

    public Optional<String> getName(List<AnnotatedElement> candidates, MessageBundle messageBundle) {
        Optional<String> explicit = candidates.stream()
                .filter(element -> element.isAnnotationPresent(GraphQLInputField.class))
                .findFirst()
                .map(element -> element.getAnnotation(GraphQLInputField.class).name());
        Optional<String> implicit = candidates.stream()
                .filter(element -> element.isAnnotationPresent(GraphQLQuery.class))
                .findFirst()
                .map(element -> element.getAnnotation(GraphQLQuery.class).name());
        return Utils.or(explicit, implicit).filter(Utils::isNotEmpty).map(messageBundle::interpolate);
    }

    public Optional<String> getDescription(List<AnnotatedElement> candidates, MessageBundle messageBundle) {
        Optional<String> explicit = candidates.stream()
                .filter(element -> element.isAnnotationPresent(GraphQLInputField.class))
                .findFirst()
                .map(element -> element.getAnnotation(GraphQLInputField.class).description());
        Optional<String> implicit = candidates.stream()
                .filter(element -> element.isAnnotationPresent(GraphQLQuery.class))
                .findFirst()
                .map(element -> element.getAnnotation(GraphQLQuery.class).description());
        return Utils.or(explicit, implicit).filter(Utils::isNotEmpty).map(messageBundle::interpolate);
    }

    public Optional<Object> defaultValue(List<AnnotatedElement> candidates, AnnotatedType type, MessageBundle messageBundle) {
        return candidates.stream()
                .filter(element -> element.isAnnotationPresent(GraphQLInputField.class))
                .findFirst()
                .map(element -> {
                    GraphQLInputField ann = element.getAnnotation(GraphQLInputField.class);
                    try {
                        return ann.defaultValueProvider().newInstance()
                                .getDefaultValue(element, type, messageBundle.interpolate(ReservedStrings.decode(ann.defaultValue())));
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new IllegalArgumentException(
                                ann.defaultValueProvider().getName() + " must expose a public default constructor", e);
                    }
                });
    }
}
