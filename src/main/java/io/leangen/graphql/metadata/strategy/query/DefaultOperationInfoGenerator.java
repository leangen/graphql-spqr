package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public class DefaultOperationInfoGenerator implements OperationInfoGenerator {

    private OperationInfoGenerator delegate = new PropertyOperationInfoGenerator();

    @Override
    public String name(OperationInfoGeneratorParams params) {
        AnnotatedElement element = params.getElement().getElements().get(0);
        if (!(element instanceof Member)) {
            throw new TypeMappingException("Only fields and methods can be mapped to GraphQL operations. " +
                    "Encountered: " + element.getClass().getName());
        }
        return Utils.coalesce(delegate.name(params), ((Member) element).getName());
    }

    @Override
    public String description(OperationInfoGeneratorParams params) {
        return delegate.description(params);
    }

    @Override
    public String deprecationReason(OperationInfoGeneratorParams params) {
        return delegate.deprecationReason(params);
    }

    @SuppressWarnings("WeakerAccess")
    public DefaultOperationInfoGenerator withDelegate(OperationInfoGenerator delegate) {
        this.delegate = delegate;
        return this;
    }
}
