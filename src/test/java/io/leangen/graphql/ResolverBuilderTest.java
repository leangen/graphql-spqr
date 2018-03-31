package io.leangen.graphql;

import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.domain.Person;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.DefaultInclusionStrategy;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.BeanResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.OperationNameGenerator;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ResolverBuilderTest {

    private static final String BASE_PACKAGE = "io.leangen";
    private static final InclusionStrategy INCLUSION_STRATEGY = new DefaultInclusionStrategy(BASE_PACKAGE);

    @Test
    public void bridgeMethodTest() {
        Collection<Resolver> resolvers = new PublicResolverBuilder(BASE_PACKAGE) .buildQueryResolvers(
                new BaseServiceImpl<Number, String>(), new TypeToken<BaseServiceImpl<Number, String>>(){}.getAnnotatedType(), INCLUSION_STRATEGY);
        assertEquals(1, resolvers.size());
        assertEquals(resolvers.iterator().next().getReturnType().getType(), Number.class);
    }

    @Test
    public void explicitIgnoreTest() {
        for(Collection<Resolver> resolvers : resolvers(new IgnoredMethods(), new BeanResolverBuilder(BASE_PACKAGE), new AnnotatedResolverBuilder())) {
            assertEquals(1, resolvers.size());
            assertEquals("notIgnored", resolvers.iterator().next().getOperationName());
        }
    }

    @Test
    public void fieldIgnoreTest() {
        for(Collection<Resolver> resolvers : resolvers(new IgnoredFields(), new BeanResolverBuilder(BASE_PACKAGE), new AnnotatedResolverBuilder())) {
            assertEquals(1, resolvers.size());
            assertEquals("notIgnored", resolvers.iterator().next().getOperationName());
        }
    }

    @Test
    public void impreciseBeanTypeTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withResolverBuilders(new PublicResolverBuilder(BASE_PACKAGE).withOperationNameGenerator(new OperationNameGenerator() {
                    @Override
                    public String generateQueryName(Method queryMethod, AnnotatedType declaringType, Object instance) {
                        return instance.getClass().getSimpleName() + "_" + queryMethod.getName();
                    }

                    @Override
                    public String generateQueryName(Field queryField, AnnotatedType declaringType, Object instance) {
                        return null;
                    }

                    @Override
                    public String generateMutationName(Method mutationMethod, AnnotatedType declaringType, Object instance) {
                        return null;
                    }

                    @Override
                    public String generateSubscriptionName(Method subscriptionMethod, AnnotatedType declaringType, Object instance) {
                        return null;
                    }
                }))
                .withOperationsFromSingleton(new One(), new TypeToken<BaseService<Person, Long>>(){}.getAnnotatedType())
                .withOperationsFromSingleton(new Two(), new TypeToken<BaseService<Number, Long>>(){}.getAnnotatedType())
                .generate();
        assertEquals(2, schema.getQueryType().getFieldDefinitions().size());
        assertEquals("One_findOne", schema.getQueryType().getFieldDefinitions().get(0).getName());
        assertEquals("Person", schema.getQueryType().getFieldDefinitions().get(0).getType().getName());
        assertEquals("Two_findOne", schema.getQueryType().getFieldDefinitions().get(1).getName());
        assertEquals("BigDecimal", schema.getQueryType().getFieldDefinitions().get(1).getType().getName());
    }

    private Collection<Collection<Resolver>> resolvers(Object bean, ResolverBuilder... builders) {
        Collection<Collection<Resolver>> resolvers = new ArrayList<>(builders.length);
        for (ResolverBuilder builder : builders) {
            resolvers.add(builder
                    .buildQueryResolvers(bean, GenericTypeReflector.annotate(bean.getClass()), INCLUSION_STRATEGY));
        }
        return resolvers;
    }

    private interface BaseService<T, ID> {

        T findOne(@GraphQLArgument(name = "id") ID id);

    }

    private static class BaseServiceImpl<T, ID extends Serializable> implements BaseService<T, ID> {

        @Override
        public T findOne(@GraphQLArgument(name = "id") ID id) {
            return null;
        }
    }

    private static class One implements BaseService<Person, Long> {

        @Override
        public Person findOne(Long aLong) {
            return new Person() {
                @Override
                public String getName() {
                    return "Dude Prime";
                }
            };
        }
    }

    private static class Two implements BaseService<Number, Long> {

        @Override
        public Number findOne(Long aLong) {
            return new BigInteger("2");
        }
    }

    private static class IgnoredMethods {

        @GraphQLQuery(name = "notIgnored")
        public String getNotIgnored() {
            return null;
        }

        @GraphQLIgnore
        @GraphQLQuery(name = "ignored")
        public String getIgnored() {
            return null;
        }
    }

    private static class IgnoredFields {

        @GraphQLIgnore
        @GraphQLQuery(name = "ignored")
        public String ignored;

        @GraphQLQuery(name = "notIgnored")
        public String getNotIgnored() {
            return null;
        }
    }
}
