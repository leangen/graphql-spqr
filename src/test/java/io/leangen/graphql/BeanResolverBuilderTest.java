package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.util.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BeanResolverBuilderTest {
    
    @Test
    public void basePackageTest() {
        PublicResolverBuilder resolverBuilder = new PublicResolverBuilder("io");
        List<Resolver> resolvers = new ArrayList<>(resolverBuilder.buildQueryResolvers(
                new UserHandleService(), GenericTypeReflector.annotate(UserHandleService.class)));
        assertEquals(2, resolvers.size());
        assertTrue(resolvers.stream().anyMatch(resolver -> resolver.getOperationName().equals("getUserHandle")));
        assertTrue(resolvers.stream().anyMatch(resolver -> resolver.getOperationName().equals("getNickname")));
    }
    
    @Test
    public void badBasePackageTest() {
        PublicResolverBuilder resolverBuilder = new PublicResolverBuilder("bad.package");
        List<Resolver> resolvers = new ArrayList<>(resolverBuilder.buildQueryResolvers(
                new UserHandleService(), GenericTypeReflector.annotate(UserHandleService.class)));
        assertEquals(1, resolvers.size());
        assertTrue(resolvers.stream().anyMatch(resolver -> resolver.getOperationName().equals("getUserHandle")));
    }
    
    @SuppressWarnings("WeakerAccess")
    public static class NicknameService {
        
        public String getNickname(String name) {
            return Utils.notEmpty(name) && name.length() > 3 ? name.substring(0, 3) : name;
        }
    }

    public static class UserHandleService extends NicknameService {

        public String getUserHandle(String name) {
            return "@" + super.getNickname(name);
        }
    }
}
