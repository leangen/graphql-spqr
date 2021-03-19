package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.TypeDiscriminatorField;
import io.leangen.graphql.metadata.exceptions.MappingException;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Set;

public class DelegatingInputFieldBuilder implements InputFieldBuilder {

    private final List<InputFieldBuilder> delegates;

    public DelegatingInputFieldBuilder(List<InputFieldBuilder> delegates) {
        this.delegates = delegates;
    }

    @Override
    public Set<InputField> getInputFields(InputFieldBuilderParams params) {
        return getBuilder(params.getType()).getInputFields(params);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return delegates.stream().anyMatch(builder -> builder.supports(type));
    }

    @Override
    public TypeDiscriminatorField getTypeDiscriminatorField(InputFieldBuilderParams params) {
        return getBuilder(params.getType()).getTypeDiscriminatorField(params);
    }

    private InputFieldBuilder getBuilder(AnnotatedType type) {
        return delegates.stream()
                .filter(builder -> builder.supports(type))
                .findFirst()
                .orElseThrow(() -> new MappingException(String.format("No %s found for type %s",
                        InputFieldBuilder.class.getSimpleName(), ClassUtils.toString(type))));
    }
}
