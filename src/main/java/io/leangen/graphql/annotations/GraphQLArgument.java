package io.leangen.graphql.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface GraphQLArgument {

    String NONE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";
    String NULL = "\n\t\t\n\t\t\n\ue000\ue001\ue002\ue003\n\t\t\t\t\n";

    String name();

    String description() default "";

    String defaultValue() default NONE;
}
