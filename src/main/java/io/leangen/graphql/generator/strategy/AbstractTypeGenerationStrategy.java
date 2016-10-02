package io.leangen.graphql.generator.strategy;

import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.QueryRepository;
import io.leangen.graphql.metadata.DomainType;

import java.util.Optional;

/**
 * Created by bojan.tomic on 3/29/16.
 */
public abstract class AbstractTypeGenerationStrategy {

    protected QueryRepository queryRepository;

    public AbstractTypeGenerationStrategy(QueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public abstract Entry get(DomainType domainType);

    public static class Entry {
        public String name;
        public Optional<GraphQLOutputType> type;

        public Entry(String name, GraphQLOutputType type) {
            this.name = name;
            this.type = Optional.ofNullable(type);
        }
    }
}
