package io.leangen.graphql.metadata.strategy.query;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public interface OperationInfoGenerator {

    String name(OperationInfoGeneratorParams params);

    String description(OperationInfoGeneratorParams params);

    String deprecationReason(OperationInfoGeneratorParams params);
}
