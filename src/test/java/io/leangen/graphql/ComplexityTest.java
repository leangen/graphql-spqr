package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.RelayTest.BookService;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.domain.Character;
import io.leangen.graphql.domain.*;
import io.leangen.graphql.execution.complexity.ComplexityFunction;
import io.leangen.graphql.execution.complexity.ComplexityLimitExceededException;
import io.leangen.graphql.execution.complexity.JavaScriptEvaluator;
import io.leangen.graphql.execution.complexity.SimpleComplexityFunction;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.execution.relay.generic.PageFactory;
import io.leangen.graphql.services.UserService;
import io.leangen.graphql.util.GraphQLUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reactivestreams.Publisher;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

import static io.leangen.graphql.support.Matchers.hasComplexityScore;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class ComplexityTest {

    private static final String unionQuery =
            "{" +
            "  character(name: \"name\") {" +
            "    ... on Human {" +
            "      name" +
            "      nickName" +
            "    }" +
            "    ... on Robot {" +
            "      name" +
            "    }" +
            "  }" +
            "}";

    private static final String mixedIntrospectionQuery =
            "{" +
            "  __schema {" +
            "    queryType {" +
            "      name" +
            "    }" +
            "    mutationType {" +
            "      name" +
            "    }" +
            "  }" +
            "  __typename" +
            "  character(name: \"name\") {" +
            "    ... on Human {" +
            "      name" +
            "      nickName" +
            "    }" +
            "    ... on Robot {" +
            "      name" +
            "    }" +
            "  }" +
            "}";

    private static final String fragmentOnRootQuery =
            "{" +
            "  ... Cat" +
            "}" +
            "" +
            "fragment Cat on Query {" +
            "  pet(cat: true) {" +
            "    sound" +
            "  }" +
            "}";

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

    private static final String branchingQuery =
            "{" +
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

    private static final String branchingFragmentQuery =
            "{" +
            "  pet(cat: true) {" +
            "    sound" +
            "    owner {" +
            "       name" +
            "    }" +
            "    ... catInfo" +
            "    ... dogInfo" +
            "  }" +
            "}" +
            "" +
            "fragment catInfo on Cat {" +
            "  id" +
            "  clawLength" +
            "}" +
            "" +
            "fragment dogInfo on Dog {" +
            "   boneCount" +
            "   owner {" +
            "       nickName" +
            "   }" +
            "}";

    private static final String multiRootQuery = "query CatDog {" +
            "  cat: pet(cat: true) {" +
            "    sound" +
            "    owner {" +
            "       name" +
            "    }" +
            "  }" +
            "  dog: pet(cat: false) {" +
            "    sound" +
            "    owner {" +
            "       name" +
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

    @Parameterized.Parameter
    public ComplexityFunction complexityFunction;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static ComplexityFunction[] data() {
        return new ComplexityFunction[] { new JavaScriptEvaluator(), new SimpleComplexityFunction() };
    }

    @Test
    public void unionComplexityTest() {
        testComplexity(
                new CharacterService(),
                unionQuery,
                2,
                3
        );
    }

    @Test
    public void fragmentOnRootQueryComplexityTest() {
        testComplexity(
                new PetService(),
                fragmentOnRootQuery,
                1,
                2
        );
    }

    @Test
    public void fragmentComplexityTest() {
        testComplexity(new UserService<Education>(), new TypeToken<UserService<Education>>(){}.getAnnotatedType(), fragmentQuery, 11, 32);
    }

    @Test
    public void branchingComplexityTest() {
        testComplexity(new PetService(), branchingQuery, 5, 6);
    }

    @Test
    public void branchingFragmentComplexityTest() {
        testComplexity(new PetService(), branchingFragmentQuery, 6, 7);
    }

    @Test
    public void multiRootComplexityTest() {
        testComplexity(new PetService(), multiRootQuery, 7, 8);
    }

    @Test
    public void connectionComplexityTest() {
        testComplexity(new PagedPetService(), pagedQuery, 50, 80);
    }

    @Test
    public void introspectionComplexityTest() {
        //introspection query complexity should be independent of the schema (service)
        testComplexity(new PetService(), GraphQLUtils.FULL_INTROSPECTION_QUERY, 107, 108);
        testComplexity(new BookService(), GraphQLUtils.FULL_INTROSPECTION_QUERY, 107, 108);
    }

    @Test
    public void mixedIntrospectionComplexityTest() {
        //introspection query complexity should be independent of the schema (service)
        testComplexity(new CharacterService(), mixedIntrospectionQuery, 8, 9);
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
        ExecutableSchema schema = new TestSchemaGenerator()
                .withAbstractInputTypeResolution()
                .withOperationsFromSingleton(service, serviceType)
                .generateExecutable();

        GraphQL exe = GraphQLRuntime.newGraphQL(schema)
                .maximumQueryComplexity(maxComplexity, complexityFunction)
                .build();

        ExecutionResult res = exe.execute(operation);
        assertEquals(1, res.getErrors().size());
        GraphQLError error = res.getErrors().get(0);
        assertTrue(error instanceof ComplexityLimitExceededException);
        assertThat((ComplexityLimitExceededException) error, hasComplexityScore(expectedComplexity));
    }

    public static class PetService {

        @GraphQLQuery(name = "pet")
        public @GraphQLNonNull Pet findPet(@GraphQLArgument(name = "cat") boolean cat) {
            return cat ? new Cat() : new Dog();
        }

        @GraphQLMutation
        @GraphQLComplexity("2 + childScore")
        public @GraphQLNonNull List<@GraphQLNonNull Pet> addPet(@GraphQLArgument(name = "pet") Pet pet) {
            return Collections.singletonList(pet);
        }

        @GraphQLSubscription
        @GraphQLComplexity("2 + childScore")
        public Publisher<Pet> newPets() {
            return null;
        }
    }

    public static class PagedPetService {

        @GraphQLQuery(name = "pets")
        public Page<Pet> findPets(@GraphQLArgument(name = "first") int first, @GraphQLArgument(name = "after") String after) {
            return PageFactory.createPage(Collections.emptyList(), PageFactory.offsetBasedCursorProvider(0L), false, false);
        }
    }

    public static class CharacterService {
        @GraphQLQuery(name = "character")
        public Character findCharacter(@GraphQLArgument(name = "name") String name) {
            return null;
        }
    }
}
