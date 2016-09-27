package io.leangen.graphql.annotations;

import java.lang.annotation.*;

/**
 * Created by bojan.tomic on 7/17/16.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GraphQLType {

    String name();

    String description() default "";
}