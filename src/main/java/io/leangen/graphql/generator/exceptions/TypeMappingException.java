package io.leangen.graphql.generator.exceptions;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

/**
 * Thrown from the mapping process when the type of the object to be mapped can not be determined.
 * Commonly occurs when type information was lost due to type erasure or dynamic proxying.
 */
public class TypeMappingException extends IllegalArgumentException {

    public TypeMappingException() {
        super("The provided object is of an unknown type. Provide the type explicitly when registering the bean.");
    }
    
    public TypeMappingException(String s) {
        super(s);
    }

    public TypeMappingException(Member fieldOrMethod, Throwable cause) {
        super("Member " + fieldOrMethod.getName() + " belonging to " + fieldOrMethod.getDeclaringClass().getName() + " has indeterminable type and can not be mapped.\n" +
                "This can be resolved either by declaring the generic types more strongly or by registering a custom mapper.", cause);
    }

    public TypeMappingException(Type superType, Type subType) {
        super(String.format("Auto discovered type %s can not be uniquely resolved as a subtype of %s",
                superType.getTypeName(), subType.getTypeName()));
    }
}
