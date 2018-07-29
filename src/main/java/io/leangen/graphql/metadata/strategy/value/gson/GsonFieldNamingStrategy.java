package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.annotations.SerializedName;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.value.InputFieldInfoGenerator;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

public class GsonFieldNamingStrategy implements FieldNamingStrategy {

    private final InputFieldInfoGenerator inputInfoGen = new InputFieldInfoGenerator();
    private final FieldNamingStrategy fallback;
    private final MessageBundle messageBundle;

    public GsonFieldNamingStrategy(MessageBundle messageBundle) {
        this(FieldNamingPolicy.IDENTITY, messageBundle);
    }

    public GsonFieldNamingStrategy(FieldNamingStrategy fallback, MessageBundle messageBundle) {
        this.fallback = fallback;
        this.messageBundle = messageBundle;
    }

    @Override
    public String translateName(Field field) {
        return getPropertyName(ClassUtils.getPropertyMembers(field))
                .orElse(fallback.translateName(field));
    }

    private Optional<String> getPropertyName(List<AnnotatedElement> candidates) {
        Optional<String> spqrName = inputInfoGen.getName(candidates, messageBundle);
        Optional<String> gsonName = candidates.stream()
                .filter(element -> element.isAnnotationPresent(SerializedName.class))
                .findFirst()
                .map(element -> element.getAnnotation(SerializedName.class).value());
        return Utils.or(spqrName, gsonName).filter(Utils::isNotEmpty);
    }
}
