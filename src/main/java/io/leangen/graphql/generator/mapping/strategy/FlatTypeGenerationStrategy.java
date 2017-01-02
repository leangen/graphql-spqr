package io.leangen.graphql.generator.mapping.strategy;

import java.util.HashSet;
import java.util.Set;

import graphql.schema.GraphQLTypeReference;
import io.leangen.graphql.generator.QueryRepository;
import io.leangen.graphql.metadata.DomainType;

public class FlatTypeGenerationStrategy extends AbstractTypeGenerationStrategy {

    private Set<String> inProgress = new HashSet<>();

    public FlatTypeGenerationStrategy(QueryRepository queryRepository) {
        super(queryRepository);
    }

    @Override
    public Entry get(DomainType domainType) {
        if (inProgress.contains(domainType.getName())) {
            return new Entry(domainType.getName(), new GraphQLTypeReference(domainType.getName()));
        }
        inProgress.add(domainType.getName());
        return new Entry(domainType.getName(), null);
    }
}
