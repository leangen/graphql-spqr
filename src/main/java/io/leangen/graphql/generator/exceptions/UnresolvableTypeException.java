package io.leangen.graphql.generator.exceptions;

import graphql.schema.GraphQLType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class UnresolvableTypeException extends IllegalStateException {

    public UnresolvableTypeException(GraphQLType fieldType, Object result) {
        super(String.format(
                "Exact GraphQL type for %s is unresolvable for an object of type %s",
                fieldType.getName(), result.getClass().getName()));
    }

    public UnresolvableTypeException(Object result) {
        super(String.format(
                "Exact GraphQL type is unresolvable for an object of type %s", result.getClass().getName()));
    }

    public UnresolvableTypeException(Object result, Exception cause) {
        super(String.format("Exception occurred during GraphQL type resolution for an object of type %s",
                result.getClass().getName()), cause);
    }
}
