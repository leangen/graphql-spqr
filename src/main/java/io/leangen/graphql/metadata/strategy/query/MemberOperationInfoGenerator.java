package io.leangen.graphql.metadata.strategy.query;

public class MemberOperationInfoGenerator extends DefaultOperationInfoGenerator {

    public MemberOperationInfoGenerator() {
        withDelegate(new AnnotatedOperationInfoGenerator());
    }
}
