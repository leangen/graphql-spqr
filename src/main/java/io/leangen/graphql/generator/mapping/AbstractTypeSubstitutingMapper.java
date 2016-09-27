package io.leangen.graphql.generator.mapping;

import graphql.schema.GraphQLOutputType;
import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.metadata.DomainType;

import java.lang.reflect.AnnotatedType;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
//The substitute type S is reflectively accessed by the default #getSubstituteType impl
@SuppressWarnings("unused")
public abstract class AbstractTypeSubstitutingMapper<S> extends DefaultTypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(DomainType domainType, List<String> parentTrail, BuildContext buildContext) {
        return super.toGraphQLType(new DomainType(getSubstituteType(domainType.getJavaType())), parentTrail, buildContext);
    }

    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), AbstractTypeSubstitutingMapper.class.getTypeParameters()[0]);
    }
}
