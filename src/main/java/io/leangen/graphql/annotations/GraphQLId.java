package io.leangen.graphql.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes the annotated element is to be mapped as a GraphQL ID
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.FIELD})
public @interface GraphQLId {
    String RELAY_ID_FIELD_NAME = "id"; //The name of the ID field, as defined by the Node interface
    
    boolean relayId() default false;
}
