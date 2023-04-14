package io.leangen.graphql;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.support.TestLog;
import org.junit.Test;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.util.List;

import static io.leangen.graphql.support.LogAssertions.assertWarningsLogged;
import static org.junit.Assert.assertTrue;

public class NameCollisionDetectionTest {

    @Test
    public void testNonColliding() {
        try (TestLog log = new TestLog(OperationMapper.class)) {
            new GraphQLSchemaGenerator()
                    .withOperationsFromSingleton(new NonColliding())
                    .generate();
            assertTrue(log.getEvents().isEmpty());
        }
    }

    @Test
    public void testColliding() {
        try (TestLog log = new TestLog(OperationMapper.class)) {
            new GraphQLSchemaGenerator()
                    .withOperationsFromSingleton(new Colliding())
                    .generate();
            assertWarningsLogged(log.getEvents(), "type name collision detected");
        }
    }

    private static class NonColliding {
        @GraphQLQuery
        public List<@Size(max = 10) String> one() {
            return null;
        }

        @GraphQLQuery
        public List<@Email String> two() {
            return null;
        }
    }

    private static class Colliding {
        @GraphQLQuery
        public One one() {
            return null;
        }

        @GraphQLQuery
        public Two two() {
            return null;
        }
    }

    @GraphQLType(name = "Collision")
    private static class One {
        public String name;
    }

    @GraphQLType(name = "Collision")
    private static class Two {
        public String name;
    }
}
