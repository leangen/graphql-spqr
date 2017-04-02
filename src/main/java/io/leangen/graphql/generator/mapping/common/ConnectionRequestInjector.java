package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.RelayConnectionRequest;
import io.leangen.graphql.execution.ConnectionRequest;
import io.leangen.graphql.execution.ResolutionContext;
import io.leangen.graphql.generator.mapping.ArgumentInjector;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ConnectionRequestInjector implements ArgumentInjector {
    
    @Override
    public Object getArgumentValue(Object rawInput, AnnotatedType type, ResolutionContext resolutionContext) {
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
