package io.leangen.graphql.annotations;

import java.lang.annotation.*;

/**
 * @deprecated Inline unions proved to be both difficult to use and maintain, while bringing limited value.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@Deprecated(forRemoval = true)

public @interface GraphQLUnion {

    String name() default "";

    String description() default "";
}
