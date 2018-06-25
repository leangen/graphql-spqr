package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLTypeResolver;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.execution.TypeResolutionEnvironment;
import io.leangen.graphql.execution.TypeResolver;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class TypeResolverTest {

    @Test
    public void testTypeResolver() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new RepoService())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        String queryTemplate = "{repo(id: %d) {" +
                "identifier,  " +
                "... on SessionRepo_Street {street: item {name}} " +
                "... on SessionRepo_Education {school: item {schoolName}}}}";
        ExecutionResult result = exe.execute(String.format(queryTemplate, 2));
        assertNoErrors(result);
        assertValueAtPathEquals("Baker street", result, "repo.0.0.street.name");
//        exe.execute("{repo(id: 3) {key {identifier  ... on SessionRepo_Street {street: item {name}} ... on SessionRepo_Education {school: item {schoolName}}}}}}", new HashMap<>());
//        result = exe.execute("{repo(id: 3) {identifier  ... on SessionRepo_Street {street: item {name}} }}");
        result = exe.execute(String.format(queryTemplate, 3));
        assertNoErrors(result);
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

    @GraphQLTypeResolver(RepoTypeResolver.class)
    public static class SessionRepo<T> implements GenericRepo {

        private T item;

        SessionRepo(T item) {
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

    public static class RepoTypeResolver implements TypeResolver {

        @Override
        public GraphQLObjectType resolveType(TypeResolutionEnvironment env) {
            String typeName = "SessionRepo_" + ((SessionRepo) env.getObject()).getStoredItem().getClass().getSimpleName();
            return (GraphQLObjectType) env.getSchema().getType(typeName);
        }
    }
}
