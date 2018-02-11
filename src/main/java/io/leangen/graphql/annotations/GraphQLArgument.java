package io.leangen.graphql.annotations;

import io.leangen.graphql.generator.mapping.strategy.DefaultValueProvider;
import io.leangen.graphql.generator.mapping.strategy.JsonDefaultValueProvider;
import io.leangen.graphql.util.Utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface GraphQLArgument {

    String NONE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";

    String name();

    String description() default "";

    String defaultValue() default Utils.NULL;
    
    Class<? extends DefaultValueProvider> defaultValueProvider() default JsonDefaultValueProvider.class;
}
