package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.RelayConnectionRequest;
import io.leangen.graphql.generator.mapping.InputValueProvider;
import io.leangen.graphql.query.ConnectionRequest;
import io.leangen.graphql.query.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ConnectionRequestProvider implements InputValueProvider {
    
    @Override
    public Object getInputValue(Object rawInput, AnnotatedType type, ResolutionContext resolutionContext) {
        if (GenericTypeReflector.isSuperType(ConnectionRequest.class, type.getType())) {
            return resolutionContext.connectionRequest;
        } else {
            return resolutionContext.connectionRequest.getParameter(type.getAnnotation(RelayConnectionRequest.class).value());
        }
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(RelayConnectionRequest.class) || GenericTypeReflector.isSuperType(ConnectionRequest.class, type.getType());
    }
}
