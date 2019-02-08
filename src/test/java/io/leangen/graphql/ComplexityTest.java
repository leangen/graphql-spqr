package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.RelayTest.BookService;
import io.leangen.graphql.annotations.GraphQLComplexity;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.Subscription;
import io.leangen.graphql.domain.Cat;
import io.leangen.graphql.domain.Dog;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Pet;
import io.leangen.graphql.execution.complexity.ComplexityLimitExceededException;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.execution.relay.generic.PageFactory;
import io.leangen.graphql.services.UserService;
import io.leangen.graphql.util.GraphQLUtils;
import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.junit.Test;
import org.reactivestreams.Publisher;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

import static io.leangen.graphql.support.Matchers.hasComplexityScore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ComplexityTest {

    private static final String fragmentQuery = "{" +
            "  newUsers: users(regDate: 1465667452785) {" +
            "    name" +
            "    home: addresses {" +
            "      streets {" +
            "        name" +
            "      }" +
            "    }" +
            "    ...userInfo" +
            "    uuid" +
            "  }" +
            "}" +
            "" +
            "fragment userInfo on User_String {" +
            "  name" +
            "  title" +
            "  regDate" +
            "  addresses {" +
            "    types" +
            "    streets {" +
            "      name" +
            "      number" +
            "    }" +
            "  }" +
            "}";

    private static final String branchingQuery = "{" +
            "  pet(cat: true) {" +
            "    sound" +
            "    owner {" +
            "       name" +
            "    }" +
            "    ... on Cat {" +
            "      clawLength" +
            "    }" +
            "    ... on Dog {" +
            "       boneCount" +
            "       owner {" +
            "           nickName @skip(if: true)" +
            "       }" +
            "    }" +
            "  }" +
            "}";

    private static final String pagedQuery = "{pets(first:10, after:\"20\") {" +
            "   pageInfo {" +
            "       hasNextPage" +
            "   }," +
            "   edges {" +
            "       cursor, node {" +
            "           sound" +
            "           owner {" +
            "               name" +
            "           }" +
            "}}}}";

    private static final String simpleMutation = "mutation AddPet {" +
            "   addPet(pet: {_type_: Cat}) {" +
            "       sound" +
            "       owner {" +
            "           name" +
            "       }" +
            "       ... on Cat {" +
            "           clawLength" +
            "       }" +
            "   }" +
            "}";

    private static final String subscription = "subscription NewArrivals {" +
            "   newPets {" +
            "       sound" +
            "       owner {" +
            "           name" +
            "       }" +
            "   }" +
            "}";

    @Test
    public void fragmentComplexityTest() {
        testComplexity(new UserService<Education>(), new TypeToken<UserService<Education>>(){}.getAnnotatedType(), fragmentQuery, 11, 32);
    }

    @Test
    public void branchingComplexityTest() {
        testComplexity(new PetService(), branchingQuery, 5, 6);
    }

    @Test
    public void connectionComplexityTest() {
        testComplexity(new PagedPetService(), pagedQuery, 50, 80);
    }

    @Test
    public void introspectionComplexityTest() {
        //introspection query complexity should be independent of the schema (service)
        testComplexity(new PetService(), GraphQLUtils.FULL_INTROSPECTION_QUERY, 108, 109);
        testComplexity(new BookService(), GraphQLUtils.FULL_INTROSPECTION_QUERY, 108, 109);
    }

    @Test
    public void mutationComplexityTest() {
        testComplexity(new PetService(), simpleMutation, 4, 6);
    }

    @Test
    public void subscriptionComplexityTest() {
        testComplexity(new PetService(), subscription, 4, 5);
    }

    private void testComplexity(Object service, String operation, int maxComplexity, int expectedComplexity) {
        testComplexity(service, GenericTypeReflector.annotate(service.getClass()), operation, maxComplexity, expectedComplexity);
    }

    private void testComplexity(Object service, AnnotatedType serviceType, String operation, int maxComplexity, int expectedComplexity) {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withAbstractInputTypeResolution()
                .withOperationsFromSingleton(service, serviceType)
                .generate();

        GraphQL exe = GraphQLRuntime.newGraphQL(schema)
                .maximumQueryComplexity(maxComplexity)
                .build();

        ExecutionResult res = exe.execute(operation);
        assertEquals(1, res.getErrors().size());
        GraphQLError error = res.getErrors().get(0);
        assertTrue(error instanceof ComplexityLimitExceededException);
        assertThat((ComplexityLimitExceededException) error, hasComplexityScore(expectedComplexity));
    }

    public static class PetService {

        @Query(value = "pet")
        public @GraphQLNonNull Pet findPet(@Argument(value = "cat") boolean cat) {
            return cat ? new Cat() : new Dog();
        }

        @Mutation
        @GraphQLComplexity("2 + childScore")
        public @GraphQLNonNull List<@GraphQLNonNull Pet> addPet(@Argument(value = "pet") Pet pet) {
            return Collections.singletonList(pet);
        }

        @Subscription
        @GraphQLComplexity("2 + childScore")
        public Publisher<Pet> newPets() {
            return null;
        }
    }

    public static class PagedPetService {

        @Query(value = "pets")
        public Page<Pet> findPets(@Argument(value = "first") int first, @Argument(value = "after") String after) {
            return PageFactory.createPage(Collections.emptyList(), PageFactory.offsetBasedCursorProvider(0L), false, false);
        }
    }
}
