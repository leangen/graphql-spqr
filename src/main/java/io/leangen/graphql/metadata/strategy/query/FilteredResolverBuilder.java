package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by bojan.tomic on 3/21/17.
 */
@SuppressWarnings("WeakerAccess")
public abstract class FilteredResolverBuilder implements ResolverBuilder {

    protected OperationNameGenerator operationNameGenerator;
    protected ResolverArgumentBuilder argumentExtractor;
    protected List<Predicate<Member>> filters = new ArrayList<>();
    
    public FilteredResolverBuilder withOperationNameGenerator(OperationNameGenerator operationNameGenerator) {
        this.operationNameGenerator = operationNameGenerator;
        return this;
    }

    public FilteredResolverBuilder withResolverArgumentBuilder(ResolverArgumentBuilder argumentExtractor) {
        this.argumentExtractor = argumentExtractor;
        return this;
    }

    @SafeVarargs
    public final FilteredResolverBuilder withFilters(Predicate<Member>... filters) {
        Collections.addAll(this.filters, filters);
        return this;
    }
    
    protected List<Predicate<Member>> getFilters() {
        return filters.isEmpty() ? Collections.singletonList(acceptAll) : filters;
    }
}
