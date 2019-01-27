package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class PropertyOperationInfoGenerator extends AnnotatedOperationInfoGenerator {

    @Override
    public String name(OperationInfoGeneratorParams params) {
        List<? extends AnnotatedElement> elements = params.getElement().getElements();
        Optional<String> field = Utils.extractInstances(elements, Field.class).findFirst().map(Field::getName);
        Optional<String> getter = Utils.extractInstances(elements, Method.class).findFirst().map(ClassUtils::getFieldNameFromGetter);

        return Utils.coalesce(super.name(params), Utils.or(field, getter).orElse(null));
    }
}
