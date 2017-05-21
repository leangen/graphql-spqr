package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;

import static java.util.Arrays.stream;

public class Operation {

    private final String name;
    private final String description;
    private final AnnotatedType javaType;
    private final List<Type> contextTypes;
    private final Map<String, Resolver> resolversByFingerprint;
    private final List<OperationArgument> arguments;
    private final boolean batched;

    public Operation(String name, AnnotatedType javaType, List<Type> contextTypes, 
                     List<OperationArgument> arguments, List<Resolver> resolvers, boolean batched) {

        if (!(resolvers.stream().allMatch(Resolver::isBatched) || resolvers.stream().noneMatch(Resolver::isBatched))) {
            throw new IllegalArgumentException("Operation \"" + name + "\" mixes regular and batched resolvers");
        }
        
        this.name = name;
        this.description = resolvers.stream().map(Resolver::getOperationDescription).filter(desc -> !desc.isEmpty()).findFirst().orElse("");
        this.javaType = javaType;
        this.contextTypes = contextTypes;
        this.resolversByFingerprint = collectResolversByFingerprint(resolvers);
        this.arguments = arguments;
        this.batched = batched;
    }
    
    public Operation unbatch() {
        return new UnbatchedOperation(this);
    }
    
    private Map<String, Resolver> collectResolversByFingerprint(List<Resolver> resolvers) {
        Map<String, Resolver> resolversByFingerprint = new HashMap<>();
        resolvers.forEach(resolver -> resolver.getFingerprints().forEach(fingerprint -> resolversByFingerprint.putIfAbsent(fingerprint, resolver)));
        return resolversByFingerprint;
    }

    public Resolver getResolver(String... argumentNames) {
        return resolversByFingerprint.get(getFingerprint(argumentNames));
    }

    public boolean isEmbeddableForType(Type type) {
        return this.contextTypes.stream().anyMatch(contextType -> GenericTypeReflector.isSuperType(contextType, type));
    }

    private String getFingerprint(String... argumentNames) {
        StringBuilder fingerPrint = new StringBuilder();
        Arrays.stream(argumentNames).sorted().forEach(fingerPrint::append);
        return fingerPrint.toString();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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
    public int hashCode() {
        int typeHash = stream(javaType.getAnnotations())
                .mapToInt(annotation -> annotation.getClass().getName().hashCode())
                .reduce((x, y) -> x ^ y).orElse(0);
        return name.hashCode() ^ typeHash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Operation)) return false;
        Operation that = (Operation) other;

        if ((that.javaType == null && this.javaType != null) || (that.javaType != null && this.javaType == null)) {
            return false;
        }

        return that.name.equals(this.name)
                && (that.javaType == null || that.javaType.equals(this.javaType));
    }

    @Override
    public String toString() {
        return name + "(" + String.join(",", arguments.stream().map(OperationArgument::getName).collect(Collectors.toList())) + ")";
    }
    
    private static class UnbatchedOperation extends Operation {
        
        private UnbatchedOperation(Operation operation) {
            super(operation.name, unbatchJavaType(operation.javaType), unbatchContextTypes(operation.contextTypes),
                    operation.arguments, new ArrayList<>(operation.getResolvers()), true);
        }

        private static AnnotatedType unbatchJavaType(AnnotatedType javaType) {
            return GenericTypeReflector.getTypeParameter(javaType, List.class.getTypeParameters()[0]);
        }
        
        private static List<Type> unbatchContextTypes(List<Type> contextTypes) {
            return contextTypes.stream()
                    .map(contextType -> GenericTypeReflector.getTypeParameter(contextType, List.class.getTypeParameters()[0]))
                    .collect(Collectors.toList());
        }
        
        @Override
        public Operation unbatch() {
            return this;
        }
    }
}
