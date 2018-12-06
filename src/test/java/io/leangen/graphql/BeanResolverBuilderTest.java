package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.DefaultInclusionStrategy;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilderParams;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeTransformer;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.util.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BeanResolverBuilderTest {

    private static final String[] BASE_PACKAGES = {"io.leangen"};
    private static final InclusionStrategy INCLUSION_STRATEGY = new DefaultInclusionStrategy(BASE_PACKAGES);
    private static final TypeTransformer TYPE_TRANSFORMER = new DefaultTypeTransformer(false, false);
    private static final GlobalEnvironment ENVIRONMENT = new TestGlobalEnvironment();

    @Test
    public void basePackageTest() {
        PublicResolverBuilder resolverBuilder = new PublicResolverBuilder(BASE_PACKAGES);
        List<Resolver> resolvers = new ArrayList<>(resolverBuilder.buildQueryResolvers(new ResolverBuilderParams(
                new UserHandleService(), GenericTypeReflector.annotate(UserHandleService.class), INCLUSION_STRATEGY, TYPE_TRANSFORMER, BASE_PACKAGES, ENVIRONMENT)));
        assertEquals(2, resolvers.size());
        assertTrue(resolvers.stream().anyMatch(resolver -> resolver.getOperationName().equals("userHandle")));
        assertTrue(resolvers.stream().anyMatch(resolver -> resolver.getOperationName().equals("nickname")));
    }

    @Test
    public void badBasePackageTest() {
        PublicResolverBuilder resolverBuilder = new PublicResolverBuilder("bad.package");
        List<Resolver> resolvers = new ArrayList<>(resolverBuilder.buildQueryResolvers(new ResolverBuilderParams(
                new UserHandleService(), GenericTypeReflector.annotate(UserHandleService.class), INCLUSION_STRATEGY, TYPE_TRANSFORMER, BASE_PACKAGES, ENVIRONMENT)));
        assertEquals(1, resolvers.size());
        assertTrue(resolvers.stream().anyMatch(resolver -> resolver.getOperationName().equals("userHandle")));
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
