package io.leangen.graphql.generator.mapping;

import io.leangen.graphql.query.conversion.OutputConverter;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Converts outputs of non-list collection types into lists. Needed because graphql-java always expects a list
 * as the result of a query of type {@link graphql.schema.GraphQLList}
 * @author Bojan Tomic (kaqqao)
 */
public class CollectionToListOutputConverter implements OutputConverter<Collection<?>, List<?>> {

    @Override
    public List<?> convertOutput(Collection<?> original) {
        return new ArrayList<>(original);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isSuperType(Collection.class, type.getType()) && !ClassUtils.isSuperType(List.class, type.getType());
    }
}
