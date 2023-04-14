package io.leangen.graphql.annotations;

import io.leangen.graphql.metadata.strategy.value.DefaultValueProvider;
import io.leangen.graphql.metadata.strategy.value.JsonDefaultValueProvider;
import io.leangen.graphql.util.ReservedStrings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
public @interface GraphQLInputField {

    String NULL = ReservedStrings.NULL;

    String name() default "";

    String description() default "";

    String defaultValue() default ReservedStrings.NONE;

    Class<? extends DefaultValueProvider> defaultValueProvider() default JsonDefaultValueProvider.class;
}
