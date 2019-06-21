package io.leangen.graphql.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Bojan Tomic (kaqqao)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GraphQLType {

    String name() default "";

    String description() default "";

    String[] fieldOrder() default {};

    String[] inputFieldOrder() default {};
}
