package io.leangen.graphql.generator.exceptions;

import java.lang.reflect.Member;

/**
 * Created by bojan.tomic on 6/6/16.
 */
public class TypeMappingException extends IllegalArgumentException {

    public TypeMappingException() {
        super();
    }

    public TypeMappingException(String s) {
        super(s);
    }

    public TypeMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeMappingException(Member fieldOrMethod, Throwable cause) {
        super("Member " + fieldOrMethod.getName() + " belonging to " + fieldOrMethod.getDeclaringClass().getCanonicalName() + " has indeterminable type and can not be mapped.\n" +
                "This can be resolved either by declaring the generic types more strongly or by registering a custom mapper.", cause);
    }
}
