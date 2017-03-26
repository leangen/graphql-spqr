package io.leangen.graphql.metadata.strategy.query;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.generator.union.Union;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.OperationArgumentDefaultValue;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.ClassUtils;

import static java.util.Arrays.stream;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class DefaultOperationBuilder implements OperationBuilder {

    @Override
    public Operation buildQuery(List<Resolver> resolvers) {
        String name = resolveName(resolvers);
        AnnotatedType javaType = resolveJavaType(name, resolvers);
        List<Type> sourceTypes = resolveSourceTypes(resolvers);
        List<OperationArgument> arguments = collectArguments(resolvers);
        List<OperationArgument> sortableArguments = collectArguments(
                resolvers.stream()
                        .filter(Resolver::supportsConnectionRequests)
                        .collect(Collectors.toList()));
        return new Operation(name, javaType, sourceTypes, arguments, sortableArguments, resolvers);
    }

    @Override
    public Operation buildMutation(List<Resolver> resolvers) {
        return buildQuery(resolvers);
    }

    protected String resolveName(List<Resolver> resolvers) {
        return resolvers.get(0).getOperationName();
    }

    protected AnnotatedType resolveJavaType(String queryName, List<Resolver> resolvers) {
        List<AnnotatedType> returnTypes = resolvers.stream()
                .map(Resolver::getReturnType)
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

    protected List<Type> resolveSourceTypes(List<Resolver> resolvers) {
        Set<Type> sourceTypes = resolvers.get(0).getSourceTypes();
        boolean allSame = resolvers.stream().map(Resolver::getSourceTypes)
                .allMatch(types -> types.size() == sourceTypes.size() && types.containsAll(sourceTypes));
        if (!allSame) {
            throw new IllegalStateException("Not all resolvers expect the same source types");
        }
        return new ArrayList<>(sourceTypes);
    }

    //TODO do annotations or overloading decide what arg is required? should that decision be externalized?
    protected List<OperationArgument> collectArguments(List<Resolver> resolvers) {
        Map<String, List<OperationArgument>> argumentsByName = resolvers.stream()
                .flatMap(resolver -> resolver.getArguments().stream()) // merge all known args for this query
                .collect(Collectors.groupingBy(OperationArgument::getName));

        return argumentsByName.keySet().stream()
                .map(argName -> new OperationArgument(
                        ClassUtils.getCommonSuperType(argumentsByName.get(argName).stream().map(OperationArgument::getJavaType).collect(Collectors.toList())),
                        argName,
                        argumentsByName.get(argName).stream().map(OperationArgument::getDescription).filter(Objects::nonNull).findFirst().orElse(""),
//						argumentsByName.get(argName).size() == resolvers.size() || argumentsByName.get(argName).stream().anyMatch(OperationArgument::isRequired),
                        argumentsByName.get(argName).stream().map(OperationArgument::getDefaultValue).filter(def -> def != OperationArgumentDefaultValue.EMPTY).findFirst().orElse(OperationArgumentDefaultValue.EMPTY),
                        argumentsByName.get(argName).stream().anyMatch(OperationArgument::isResolverSource),
                        argumentsByName.get(argName).stream().anyMatch(OperationArgument::isContext),
                        argumentsByName.get(argName).stream().anyMatch(OperationArgument::isRelayConnection)
                ))
                .collect(Collectors.toList());
    }

    protected AnnotatedType unionize(AnnotatedType[] types) {
        return Union.unionize(types);
    }
}
