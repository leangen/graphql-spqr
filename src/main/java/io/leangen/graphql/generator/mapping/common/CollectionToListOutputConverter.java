package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.query.ResolutionContext;

/**
 * Converts outputs of non-list collection types into lists. Needed because graphql-java always expects a list
 * as the result of a query of type {@link graphql.schema.GraphQLList}
 * @author Bojan Tomic (kaqqao)
 */
public class CollectionToListOutputConverter implements OutputConverter<Collection<?>, List<?>> {

    @Override
    public List<?> convertOutput(Collection<?> original, AnnotatedType type, ResolutionContext resolutionContext) {
        AnnotatedType inner = GenericTypeReflector.getTypeParameter(type, Collection.class.getTypeParameters()[0]);
        return original.stream()
                .map(item -> resolutionContext.convertOutput(item, inner))
                .collect(Collectors.toList());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Collection.class, type.getType());
    }
}
