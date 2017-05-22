package io.leangen.graphql;

import org.junit.Test;

import java.io.Serializable;
import java.util.Collection;

import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;

import static org.junit.Assert.assertEquals;

public class ResolverBuilderTest {
    
    @Test
    public void bridgeMethodTest() {
        Collection<Resolver> resolvers = new PublicResolverBuilder("io.leangen")
                .buildQueryResolvers(new BaseServiceImpl<Number, String>(), new TypeToken<BaseServiceImpl<Number, String>>(){}.getAnnotatedType());
        assertEquals(1, resolvers.size());
        assertEquals(resolvers.iterator().next().getReturnType().getType(), Number.class);
    }
    
    public interface BaseService<T, ID> {

        T findOne(@GraphQLArgument(name = "id") ID id);

    }

    public static class BaseServiceImpl<T, ID extends Serializable> implements BaseService<T, ID> {

        @Override
        public T findOne(@GraphQLArgument(name = "id") ID id) {
            return null;
        }
    }
}
