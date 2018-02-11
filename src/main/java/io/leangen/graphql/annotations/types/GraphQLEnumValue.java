package io.leangen.graphql.annotations.types;

import io.leangen.graphql.util.Utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GraphQLEnumValue {

    String name() default "";

    String description() default "";

    String deprecationReason() default Utils.NULL;
}
