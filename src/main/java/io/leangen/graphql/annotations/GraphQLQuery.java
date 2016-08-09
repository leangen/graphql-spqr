package io.leangen.graphql.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by bojan.tomic on 3/2/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface GraphQLQuery {

	String ROOT_QUERY_ALIAS = "ROOT_QUERY";

	String name() default "";
	String description() default "";
	Class<?> wrapper() default Void.class;
	String attribute() default "";
	String[] parentQueries() default {ROOT_QUERY_ALIAS};
}