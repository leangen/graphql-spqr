package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;

public class RecordLikeResolverBuilder extends RecordResolverBuilder {

    public RecordLikeResolverBuilder(String... basePackages) {
        super(basePackages);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true; //Unlike RecordResolverBuilder, this works on all classes
    }
}
