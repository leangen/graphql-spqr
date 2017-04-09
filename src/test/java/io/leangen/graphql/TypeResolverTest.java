package io.leangen.graphql;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLTypeHintProvider;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.generator.TypeHintProvider;

import static io.leangen.graphql.assertions.QueryResultAssertions.assertValueAtPathEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class TypeResolverTest {

    @Test
    public void testTypeHintProvider() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new RepoService())
                .generate();

        GraphQL exe = new GraphQL(schema);
        String queryTemplate = "{repo(id: %d) {" +
                "identifier  " +
                "... on SessionRepo_Street {street: item {name}} " +
                "... on SessionRepo_Education {school: item {schoolName}}}}";
        ExecutionResult result = exe.execute(String.format(queryTemplate, 2));
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("Baker street", result, "repo.0.0.street.name");
//        exe.execute("{repo(id: 3) {key {identifier  ... on SessionRepo_Street {street: item {name}} ... on SessionRepo_Education {school: item {schoolName}}}}}}", new HashMap<>());
//        result = exe.execute("{repo(id: 3) {identifier  ... on SessionRepo_Street {street: item {name}} }}");
        result = exe.execute(String.format(queryTemplate, 3));
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("Alma Mater", result, "repo.0.0.school.schoolName");
    }

    public static class RepoService {

        @GraphQLQuery(name = "repo")
        public List<Stream<GenericRepo>> getRepo(@GraphQLArgument(name = "id") int id) {
            if (id % 2 == 0) {
                return Collections.singletonList(Stream.of(new SessionRepo<>(new Street("Baker street", 1))));
            } else {
                return Collections.singletonList(Stream.of(new SessionRepo<>(new Education("Alma Mater", 1600, 1604))));
            }
        }

//    @GraphQLQuery(name = "repo")
//    public Map<GenericRepo, GenericRepo> getRepo(@GraphQLArgument(name = "id") int id) {
//        if (id % 2 == 0) {
//            return Collections.singletonMap(new SessionRepo<>(new Street("juuu", 1)), new SessionRepo<>(new Street("juuu", 1)));
//        } else {
//            return Collections.singletonMap(new SessionRepo<>(new Education("alma mater", 1600, 1604)), new SessionRepo<>(new Education("alma mater", 1600, 1604)));
//        }
//    }

        @GraphQLQuery(name = "repo1")
        public SessionRepo<Street> repo1() {
            return null;
        }

        @GraphQLQuery(name = "repo2")
        public SessionRepo<Education> repo2() {
            return null;
        }
    }

    @GraphQLInterface(name = "GenericRepository")
    private interface GenericRepo {

        @GraphQLQuery(name = "identifier")
        String identifier();
    }

    @GraphQLTypeHintProvider(RepoTypeHintProvier.class)
    public static class SessionRepo<T> implements GenericRepo {

        private T item;

        public SessionRepo(T item) {
            this.item = item;
        }

        @GraphQLQuery(name = "item")
        public T getStoredItem() {
            return item;
        }

        @Override
        @GraphQLQuery(name = "identifier")
        public String identifier() {
            return "SESSION";
        }
    }

    public static class RepoTypeHintProvier implements TypeHintProvider {

        @Override
        public String getGraphQLTypeHint(Object result, TypeResolutionEnvironment env) {
            return "SessionRepo_" + ((SessionRepo) result).getStoredItem().getClass().getSimpleName();
        }
    }
}
