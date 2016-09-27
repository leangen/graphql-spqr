package io.leangen.graphql.generator.mapping;

import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.metadata.DomainType;

import java.lang.reflect.AnnotatedType;
import java.util.List;

/**
 * Created by bojan.tomic on 9/21/16.
 */
public interface TypeMapper {

    GraphQLOutputType toGraphQLType(DomainType domainType, List<String> parentTrail, BuildContext buildContext);

    boolean supports(AnnotatedType type);
}