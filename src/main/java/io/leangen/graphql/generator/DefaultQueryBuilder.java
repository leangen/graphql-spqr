package io.leangen.graphql.generator;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.generator.union.Union;
import io.leangen.graphql.metadata.Query;
import io.leangen.graphql.metadata.QueryArgument;
import io.leangen.graphql.metadata.QueryResolver;
import io.leangen.graphql.util.ClassUtils;

import static java.util.Arrays.stream;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class DefaultQueryBuilder implements QueryBuilder {

    @Override
    public Query buildQuery(List<QueryResolver> resolvers) {
        String name = resolveName(resolvers);
        AnnotatedType javaType = resolveJavaType(name, resolvers);
        Type sourceType = resolveSourceType(resolvers);
        List<QueryArgument> arguments = collectArguments(resolvers);
        List<QueryArgument> sortableArguments = collectArguments(
                resolvers.stream()
                        .filter(QueryResolver::supportsConnectionRequests)
                        .collect(Collectors.toList()));
        return new Query(name, javaType, sourceType, arguments, sortableArguments, resolvers);
    }

    @Override
    public Query buildMutation(List<QueryResolver> resolvers) {
        return buildQuery(resolvers);
    }

    protected String resolveName(List<QueryResolver> resolvers) {
        return resolvers.get(0).getQueryName();
    }

    protected AnnotatedType resolveJavaType(String queryName, List<QueryResolver> resolvers) {
        List<AnnotatedType> returnTypes = resolvers.stream()
                .map(QueryResolver::getReturnType)
                .collect(Collectors.toList());

        if (resolvers.stream().anyMatch(resolver -> ClassUtils.containsAnnotation(resolver.getReturnType(), GraphQLUnion.class))) {
            return unionize(returnTypes.toArray(new AnnotatedType[returnTypes.size()]));
        }

        AnnotatedType mostSpecificSuperType = ClassUtils.getCommonSuperType(returnTypes);
        if (mostSpecificSuperType.getType() == Object.class || mostSpecificSuperType.getType() == Cloneable.class || mostSpecificSuperType.getType() == Serializable.class) {
            throw new IllegalArgumentException("Resolvers for query " + queryName + " do not return compatible types");
        }
        Annotation[] aggregatedAnnotations = resolvers.stream()
                .flatMap(resolver -> stream(resolver.getReturnType().getAnnotations()))
                .distinct()
                .toArray(Annotation[]::new);
        return GenericTypeReflector.replaceAnnotations(mostSpecificSuperType, aggregatedAnnotations);
    }

    protected Type resolveSourceType(List<QueryResolver> resolvers) {
        List<Type> sourceTypes = resolvers.stream()
                .map(QueryResolver::getSourceType)
                .distinct()
                .collect(Collectors.toList());
        if (sourceTypes.size() > 1) {
            throw new IllegalStateException("Not all resolvers expect the same source type");
        }
        return sourceTypes.get(0);
    }

    //TODO do annotations or overloading decide what arg is required? should that decision be externalized?
    protected List<QueryArgument> collectArguments(List<QueryResolver> resolvers) {
        Map<String, List<QueryArgument>> argumentsByName = resolvers.stream()
                .flatMap(resolver -> resolver.getQueryArguments().stream()) // merge all known args for this query
                .collect(Collectors.groupingBy(QueryArgument::getName));

        return argumentsByName.keySet().stream()
                .map(argName -> new QueryArgument(
                        ClassUtils.getCommonSuperType(argumentsByName.get(argName).stream().map(QueryArgument::getJavaType).collect(Collectors.toList())),
                        argName,
                        argumentsByName.get(argName).stream().map(QueryArgument::getDescription).filter(desc -> desc != null).findFirst().orElse(""),
//						argumentsByName.get(argName).size() == resolvers.size() || argumentsByName.get(argName).stream().anyMatch(QueryArgument::isRequired),
                        argumentsByName.get(argName).stream().anyMatch(QueryArgument::isRequired),
                        argumentsByName.get(argName).stream().anyMatch(QueryArgument::isResolverSource),
                        argumentsByName.get(argName).stream().anyMatch(QueryArgument::isContext),
                        argumentsByName.get(argName).stream().anyMatch(QueryArgument::isRelayConnection)
                ))
                .collect(Collectors.toList());
    }

    protected AnnotatedType unionize(AnnotatedType[] types) {
        return Union.unionize(types);
    }
}
