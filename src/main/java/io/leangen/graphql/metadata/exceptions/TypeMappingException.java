package io.leangen.graphql.metadata.exceptions;

import io.leangen.graphql.util.Urls;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * Thrown from the mapping process when the type of the object to be mapped can not be determined.
 * Commonly occurs when type information was lost due to type erasure or dynamic proxying.
 */
public class TypeMappingException extends MappingException {

    public TypeMappingException(String s) {
        super(s);
    }

    public TypeMappingException(String s, Exception cause) {
        super(s, cause);
    }

    public static TypeMappingException unknownType() {
        return new TypeMappingException("The provided object is of an unknown type. Provide the type explicitly when registering the bean.");
    }

    public static TypeMappingException ambiguousType(Type type) {
        return new TypeMappingException("Type " + type.getTypeName() + " is unbounded or missing generic type parameters");
    }

    public static TypeMappingException ambiguousMemberType(Member fieldOrMethod, AnnotatedType declaringType, Exception cause) {
        return new TypeMappingException("The type of member \"" + fieldOrMethod.getName() + "\" belonging to " + declaringType.getType().getTypeName() +
                " is missing generic type parameters and can not be mapped." +
                " For details and possible solutions see " + Urls.Errors.AMBIGUOUS_MEMBER_TYPE, cause);
    }

    public static TypeMappingException ambiguousParameterType(Executable executable, Parameter parameter, Exception cause) {
        return new TypeMappingException("Parameter \"" + parameter.getName() + "\" of method \"" + executable.getName() +
                "\" is missing generic type parameters and can not be mapped." +
                " For details and possible solutions see " + Urls.Errors.AMBIGUOUS_PARAMETER_TYPE, cause);
    }

    public static TypeMappingException unresolvableSuperType(Type superType, Type subType) {
        return new TypeMappingException(String.format("Type %s can not be resolved as a super type of %s." +
                " For details and possible solutions see " + Urls.Errors.UNRESOLVABLE_SUPER_TYPE, superType, subType));
    }
}
