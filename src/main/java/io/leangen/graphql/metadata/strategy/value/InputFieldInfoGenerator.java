package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.DefaultValue;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ClassUtils;
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

    public DefaultValue defaultValue(List<? extends AnnotatedElement> candidates, AnnotatedType type, GlobalEnvironment environment) {
        Optional<? extends AnnotatedElement> match = candidates.stream()
                .filter(element -> element.isAnnotationPresent(GraphQLInputField.class))
                .findFirst();

        if (!match.isPresent()) return DefaultValue.EMPTY;

        GraphQLInputField ann = match.get().getAnnotation(GraphQLInputField.class);
        try {
            return ClassUtils.instance(ann.defaultValueProvider())
                    .getDefaultValue(match.get(), type, ReservedStrings.decodeDefault(environment.messageBundle.interpolate(ann.defaultValue())));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    ann.defaultValueProvider().getName() + " must expose a public default constructor", e);
        }
    }
}
