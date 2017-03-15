package io.leangen.graphql.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.leangen.graphql.generator.TypeHintProvider;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GraphQLTypeHintProvider {

    Class<? extends TypeHintProvider> value();
}
