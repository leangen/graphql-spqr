package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

public class InputFieldBuilderParams {

    private final AnnotatedType type;
    private final InclusionStrategy inclusionStrategy;
    private final TypeTransformer typeTransformer;
    private final GlobalEnvironment environment;

    /**
     * @param type Java type (used as query input) to be analyzed for deserializable fields
     * @param inclusionStrategy The strategy that decides which input fields are acceptable
     * @param typeTransformer Transformer used to pre-process the types (can be used to complete the missing generics etc)
     * @param environment The global environment
     */
    private InputFieldBuilderParams(AnnotatedType type, InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer, GlobalEnvironment environment) {
        this.type = Objects.requireNonNull(type);
        this.inclusionStrategy = Objects.requireNonNull(inclusionStrategy);
        this.typeTransformer = Objects.requireNonNull(typeTransformer);
        this.environment = Objects.requireNonNull(environment);
    }

    public static Builder builder() {
        return new Builder();
    }

    public AnnotatedType getType() {
        return type;
    }

    public InclusionStrategy getInclusionStrategy() {
        return inclusionStrategy;
    }

    public TypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    public GlobalEnvironment getEnvironment() {
        return environment;
    }

    public static class Builder {
        private AnnotatedType type;
        private InclusionStrategy inclusionStrategy;
        private TypeTransformer typeTransformer;
        private GlobalEnvironment environment;

        public Builder withType(AnnotatedType type) {
            this.type = type;
            return this;
        }

        public Builder withInclusionStrategy(InclusionStrategy inclusionStrategy) {
            this.inclusionStrategy = inclusionStrategy;
            return this;
        }

        public Builder withTypeTransformer(TypeTransformer typeTransformer) {
            this.typeTransformer = typeTransformer;
            return this;
        }

        public Builder withEnvironment(GlobalEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public InputFieldBuilderParams build() {
            return new InputFieldBuilderParams(type, inclusionStrategy, typeTransformer, environment);
        }
    }
}
