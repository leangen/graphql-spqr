package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.Member;

public class RecordComponentInfoGenerator extends AnnotatedOperationInfoGenerator {

    @Override
    public String name(OperationInfoGeneratorParams params) {
        return params.getElement().getElements().stream()
                .filter(element -> element instanceof Member)
                .map(member -> ((Member) member).getName())
                .findAny()
                .orElse(null);
    }
}
