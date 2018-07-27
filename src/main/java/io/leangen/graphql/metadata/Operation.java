package io.leangen.graphql.metadata;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Operation {

    private final String name;
    private final String description;
    private final String deprecationReason;
    private final AnnotatedType javaType;
    private final Type contextType;
    private final Map<String, Resolver> resolversByFingerprint;
    private final List<OperationArgument> arguments;
    private final boolean batched;

    public Operation(String name, AnnotatedType javaType, Type contextType, 
                     List<OperationArgument> arguments, List<Resolver> resolvers, boolean batched) {

        if (!(resolvers.stream().allMatch(Resolver::isBatched) || resolvers.stream().noneMatch(Resolver::isBatched))) {
            throw new IllegalArgumentException("Operation \"" + name + "\" mixes regular and batched resolvers");
        }
        
        this.name = name;
        this.description = resolvers.stream().map(Resolver::getOperationDescription).filter(Utils::isNotEmpty).findFirst().orElse(null);
        this.deprecationReason = resolvers.stream().map(Resolver::getOperationDeprecationReason).filter(Objects::nonNull).findFirst().orElse(null);
        this.javaType = javaType;
        this.contextType = contextType;
        this.resolversByFingerprint = collectResolversByFingerprint(resolvers);
        this.arguments = arguments;
        this.batched = batched;
    }
    
    public Operation unbatch() {
        return batched ? new UnbatchedOperation(this) : this;
    }
    
    private Map<String, Resolver> collectResolversByFingerprint(List<Resolver> resolvers) {
        Map<String, Resolver> resolversByFingerprint = new HashMap<>();
        resolvers.forEach(resolver -> resolver.getFingerprints().forEach(fingerprint -> resolversByFingerprint.putIfAbsent(fingerprint, resolver)));
        return resolversByFingerprint;
    }

    public Resolver getApplicableResolver(Set<String> argumentNames) {
        if (resolversByFingerprint.size() == 1) {
            return getResolvers().iterator().next();
        } else {
            return resolversByFingerprint.get(getFingerprint(argumentNames));
        }
    }

    public Resolver getResolver(String... argumentNames) {
        return resolversByFingerprint.get(getFingerprint(new HashSet<>(Arrays.asList(argumentNames))));
    }

    public boolean isEmbeddableForType(Type type) {
        return contextType != null && GenericTypeReflector.isSuperType(contextType, type);
    }

    public boolean isRoot() {
        return this.contextType == null;
    }
    
    private String getFingerprint(Set<String> argumentNames) {
        StringBuilder fingerPrint = new StringBuilder();
        argumentNames.stream().sorted().forEach(fingerPrint::append);
        return fingerPrint.toString();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    public List<OperationArgument> getArguments() {
        return arguments;
    }

    public Collection<Resolver> getResolvers() {
        return resolversByFingerprint.values();
    }

    public boolean isBatched() {
        return batched;
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Operation)) return false;
        Operation that = (Operation) other;

        if (!name.equals(that.name)) return false;
        if (javaType != null ? !javaType.equals(that.javaType) : that.javaType != null)
            return false;
        return contextType != null ? contextType.equals(that.contextType) : that.contextType == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (javaType != null ? javaType.hashCode() : 0);
        result = 31 * result + (contextType != null ? contextType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name + "(" + String.join(",", arguments.stream().map(OperationArgument::getName).collect(Collectors.toList())) + ")";
    }
    
    private static class UnbatchedOperation extends Operation {
        
        private UnbatchedOperation(Operation operation) {
            super(operation.name, unbatchJavaType(operation.javaType), unbatchContextType(operation.contextType),
                    operation.arguments, new ArrayList<>(operation.getResolvers()), true);
        }

        private static AnnotatedType unbatchJavaType(AnnotatedType javaType) {
            return GenericTypeReflector.getTypeParameter(javaType, List.class.getTypeParameters()[0]);
        }
        
        private static Type unbatchContextType(Type contextType) {
            return GenericTypeReflector.getTypeParameter(contextType, List.class.getTypeParameters()[0]);
        }
        
        @Override
        public Operation unbatch() {
            return this;
        }
    }
}
