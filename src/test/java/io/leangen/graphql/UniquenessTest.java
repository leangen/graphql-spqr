package io.leangen.graphql;

import org.junit.Test;

import io.leangen.graphql.domain.SimpleUser;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.metadata.strategy.query.BeanResolverBuilder;

public class UniquenessTest {

    private static final String thisPackage = UniquenessTest.class.getPackage().getName();

    @Test
    public void testPageUniqueness() {
        new PreconfiguredSchemaGenerator()
                .withOperationsFromSingleton(new PagingService(), new BeanResolverBuilder(thisPackage))
                .generate();
    }

    @Test
    public void testEnumUniqueness() {
        new PreconfiguredSchemaGenerator()
                .withOperationsFromSingleton(new EnumService(), new BeanResolverBuilder(thisPackage))
                .generate();
    }

    public static class EnumService {

        enum BLACK_OR_WHITE {
            BLACK, WHITE
        }

        public BLACK_OR_WHITE getEnum() {
            return BLACK_OR_WHITE.BLACK;
        }

        public BLACK_OR_WHITE getEnumAgain() {
            return BLACK_OR_WHITE.WHITE;
        }
    }

    public static class PagingService {

        public Page<SimpleUser> getUsers() {
            return null;
        }

        public Page<SimpleUser> getUsersAgain() {
            return null;
        }
    }
}
