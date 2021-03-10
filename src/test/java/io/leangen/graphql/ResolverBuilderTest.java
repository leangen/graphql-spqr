package io.leangen.graphql;

import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.domain.Person;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.DefaultInclusionStrategy;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.BeanResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.OperationInfoGenerator;
import io.leangen.graphql.metadata.strategy.query.OperationInfoGeneratorParams;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilderParams;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeTransformer;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import lombok.Getter;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.leangen.graphql.support.GraphQLTypeAssertions.assertFieldNamesEqual;
import static io.leangen.graphql.util.GraphQLUtils.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResolverBuilderTest {

    private static final String[] BASE_PACKAGES = { "io.leangen" };
    private static final InclusionStrategy INCLUSION_STRATEGY = new DefaultInclusionStrategy(BASE_PACKAGES);
    private static final TypeTransformer TYPE_TRANSFORMER = new DefaultTypeTransformer(false, false);
    private static final GlobalEnvironment ENVIRONMENT = new TestGlobalEnvironment();

    @Test
    public void bridgeMethodTest() {
        Collection<Resolver> resolvers = new PublicResolverBuilder(BASE_PACKAGES).buildQueryResolvers(new ResolverBuilderParams(
                BaseServiceImpl::new, new TypeToken<BaseServiceImpl<Number, String>>(){}.getAnnotatedType(),
                BaseServiceImpl.class, INCLUSION_STRATEGY, TYPE_TRANSFORMER, BASE_PACKAGES, ENVIRONMENT));
        assertEquals(1, resolvers.size());
        assertEquals(resolvers.iterator().next().getReturnType().getType(), Number.class);
    }

    @Test
    public void explicitIgnoreTest() {
        for(Collection<Resolver> resolvers : resolvers(new IgnoredMethods(), new BeanResolverBuilder(BASE_PACKAGES), new AnnotatedResolverBuilder())) {
            assertEquals(1, resolvers.size());
            assertEquals("notIgnored", resolvers.iterator().next().getOperationName());
        }
    }

    @Test
    public void fieldIgnoreTest() {
        for(Collection<Resolver> resolvers : resolvers(new IgnoredFields<>(), new BeanResolverBuilder(BASE_PACKAGES), new AnnotatedResolverBuilder())) {
            assertEquals(1, resolvers.size());
            assertEquals("notIgnored", resolvers.iterator().next().getOperationName());
        }
    }

    @Test
    public void parameterIgnoreTest() {
        for(Collection<Resolver> resolvers : resolvers(new IgnoredParameters<>(), new PublicResolverBuilder(BASE_PACKAGES), new AnnotatedResolverBuilder())) {
            Resolver resolver = resolvers.iterator().next();
            assertEquals(1, resolver.getArguments().size());
            assertEquals("notIgnored", resolver.getArguments().get(0).getName());
        }
    }

    @Test
    public void impreciseBeanTypeTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withResolverBuilders(new PublicResolverBuilder(BASE_PACKAGES).withOperationInfoGenerator(new OperationInfoGenerator() {
                    @Override
                    public String name(OperationInfoGeneratorParams params) {
                        return params.getInstanceSupplier().get().getClass().getSimpleName() + "_" + ((Member)params.getElement().getElement()).getName();
                    }

                    @Override
                    public String description(OperationInfoGeneratorParams params) {
                        return null;
                    }

                    @Override
                    public String deprecationReason(OperationInfoGeneratorParams params) {
                        return null;
                    }
                }))
                .withOperationsFromSingleton(new One(), new TypeToken<BaseService<Person, Long>>(){}.getAnnotatedType())
                .withOperationsFromSingleton(new Two(), new TypeToken<BaseService<Number, Long>>(){}.getAnnotatedType())
                .generate();
        assertEquals(2, schema.getQueryType().getFieldDefinitions().size());
        assertEquals("One_findOne", schema.getQueryType().getFieldDefinitions().get(0).getName());
        assertEquals("Person", name(schema.getQueryType().getFieldDefinitions().get(0).getType()));
        assertEquals("Two_findOne", schema.getQueryType().getFieldDefinitions().get(1).getName());
        assertEquals("BigDecimal", name(schema.getQueryType().getFieldDefinitions().get(1).getType()));
    }

    @Test
    public void typeMergeTest() {
        ResolverBuilder[] allBuilders = new ResolverBuilder[] {
                new PublicResolverBuilder(BASE_PACKAGES), new BeanResolverBuilder(BASE_PACKAGES), new AnnotatedResolverBuilder()};
        for(Collection<Resolver> resolvers : resolvers(new MergedTypes(), allBuilders)) {
            assertEquals(2, resolvers.size());

            Optional<AnnotatedType> field1 = resolvers.stream().filter(res -> "field1".equals(res.getOperationName())).findFirst()
                    .map(Resolver::getReturnType);
            assertTrue(field1.isPresent());
            assertTrue(field1.get().isAnnotationPresent(GraphQLNonNull.class));

            Optional<AnnotatedType> field2 = resolvers.stream().filter(res -> "field2".equals(res.getOperationName())).findFirst()
                    .map(Resolver::getReturnType);
            assertTrue(field2.isPresent());
            assertTrue(field2.get().isAnnotationPresent(GraphQLNonNull.class));
        }
    }

    @Test
    public void privateFieldHierarchyTest() {
        ResolverBuilder[] allBuilders = new ResolverBuilder[] {
                new PublicResolverBuilder(BASE_PACKAGES), new BeanResolverBuilder(BASE_PACKAGES), new AnnotatedResolverBuilder()};
        for(Collection<Resolver> resolvers : resolvers(new Concrete(), allBuilders)) {
            assertEquals(3, resolvers.size());
            Stream.of("abstractField", "concreteField", "thing").forEach(field ->
                assertTrue(resolvers.stream().anyMatch(res -> res.getOperationName().equals(field)))
            );
        }
    }

    @Test
    public void typeSpecificResolverBuilderTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withResolverBuilders((config, current) -> current.prepend(new RelaxedBeanResolverBuilder()))
                .withOperationsFromSingletons(new StrangelyNamedProperties(), new StrangelyNamedProperties2())
                .generate();

        assertFieldNamesEqual(schema.getQueryType(), "name", "hasName", "title");
    }

    @Test
    public void basePackageTest() {
        PublicResolverBuilder resolverBuilder = new PublicResolverBuilder(BASE_PACKAGES);
        List<Resolver> resolvers = new ArrayList<>(resolverBuilder.buildQueryResolvers(new ResolverBuilderParams(
                UserHandleService::new, GenericTypeReflector.annotate(UserHandleService.class), UserHandleService.class, INCLUSION_STRATEGY, TYPE_TRANSFORMER, BASE_PACKAGES, ENVIRONMENT)));
        assertEquals(2, resolvers.size());
        assertTrue(resolvers.stream().anyMatch(resolver -> resolver.getOperationName().equals("userHandle")));
        assertTrue(resolvers.stream().anyMatch(resolver -> resolver.getOperationName().equals("nickname")));
    }

    @Test
    public void badBasePackageTest() {
        PublicResolverBuilder resolverBuilder = new PublicResolverBuilder("bad.package");
        List<Resolver> resolvers = new ArrayList<>(resolverBuilder.buildQueryResolvers(new ResolverBuilderParams(
                UserHandleService::new, GenericTypeReflector.annotate(UserHandleService.class), UserHandleService.class, INCLUSION_STRATEGY, TYPE_TRANSFORMER, BASE_PACKAGES, ENVIRONMENT)));
        assertEquals(1, resolvers.size());
        assertTrue(resolvers.stream().anyMatch(resolver -> resolver.getOperationName().equals("userHandle")));
    }

    private Collection<Collection<Resolver>> resolvers(Object bean, ResolverBuilder... builders) {
        Collection<Collection<Resolver>> resolvers = new ArrayList<>(builders.length);
        for (ResolverBuilder builder : builders) {
            resolvers.add(builder.buildQueryResolvers(new ResolverBuilderParams(
                    () -> bean, GenericTypeReflector.annotate(bean.getClass()), bean.getClass(), INCLUSION_STRATEGY, TYPE_TRANSFORMER, BASE_PACKAGES, ENVIRONMENT)));
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
        @SuppressWarnings("Convert2Lambda")
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

        @GraphQLIgnore
        @GraphQLQuery(name = "ignoredToo")
        public <T> T wouldBreak() { return null; }
    }

    private static class IgnoredFields<T> {

        @GraphQLIgnore
        @GraphQLQuery(name = "ignored")
        public String ignored;

        @GraphQLIgnore
        @GraphQLQuery(name = "ignoredToo")
        public T wouldBreak;

        @GraphQLQuery(name = "notIgnored")
        public String getNotIgnored() {
            return null;
        }
    }

    private static class IgnoredParameters<T> {

        @GraphQLQuery
        public String notIgnored(String notIgnored, @GraphQLIgnore T wouldBreak) {
            return null;
        }
    }

    @Getter
    private static class MergedTypes {
        @GraphQLQuery(name = "field1")
        private @GraphQLNonNull Object badName;
        private Object field2;
        private @GraphQLIgnore Object ignored;

        @GraphQLQuery
        public @GraphQLNonNull Object getField2() {
            return field2;
        }
    }

    private interface Interface {
        Object getThing();
    }

    @Getter
    private static abstract class Abstract implements Interface {
        @GraphQLQuery(name = "abstractField")
        private String a1;
    }

    @Getter
    private static class Concrete extends Abstract {
        @GraphQLQuery
        private Object thing;
        @GraphQLQuery(name = "concreteField")
        private String c1;
    }

    private static class RelaxedBeanResolverBuilder extends BeanResolverBuilder {

        @Override
        protected boolean isQuery(Method method, ResolverBuilderParams params) {
            return (method.getReturnType() == Boolean.TYPE && method.getName().startsWith("has")) || super.isQuery(method, params);
        }

        @Override
        public boolean supports(AnnotatedType type) {
            return ClassUtils.isSuperClass(StrangelyNamedProperties.class, type);
        }
    }

    private static class StrangelyNamedProperties {

        public String getName() {
            return "xyz";
        }

        public boolean hasName() {
            return true;
        }
    }

    private static class StrangelyNamedProperties2 {

        @GraphQLQuery
        public String getTitle() {
            return "xyz";
        }

        public boolean hasTitle() {
            return true;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class NicknameService {

        public String getNickname(String name) {
            return Utils.isNotEmpty(name) && name.length() > 3 ? name.substring(0, 3) : name;
        }
    }

    public static class UserHandleService extends NicknameService {

        public String getUserHandle(String name) {
            return "@" + super.getNickname(name);
        }
    }
}
