package io.leangen.graphql.generator.strategy;

import graphql.schema.GraphQLTypeReference;
import io.leangen.graphql.generator.QueryRepository;
import io.leangen.graphql.metadata.DomainType;

import java.util.List;
import java.util.Optional;

/**
 * Created by bojan.tomic on 3/30/16.
 */
public class CircularTypeGenerationStrategy extends CappedTypeGenerationStrategy {

    public CircularTypeGenerationStrategy(QueryRepository queryRepository) {
        super(queryRepository);
    }

    //TODO find the first matching type from *this* parent trail, and only then fall back to simple key
    @Override
    protected Optional<Entry> getFallback(DomainType domainType, List<String> parentTrail, String fingerprint) {
        if (fingerprint.equals(domainType.getName()) && fingerprints.containsValue(fingerprint)) {
            return Optional.of(new Entry(fingerprint, new GraphQLTypeReference(fingerprint)));
        }
        return Optional.empty();
    }
}
