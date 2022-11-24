package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLTypeResolver;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Street;
import org.junit.Test;

import java.util.Arrays;
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

    @Test
    public void testTypeResolutionWithBaseClass() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new RootQuery())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult res = exe.execute("{contents {id title}}");
        assertNoErrors(res);
    }

    @Test
    public void testTypeResolutionWithoutBaseClass() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new RootQuery2())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult res = exe.execute("{contents {id title}}");
        assertNoErrors(res);
    }

    public static class RootQuery {
        @GraphQLQuery
        public List<Content> contents() {
            return Arrays.asList(new Trailer("1", "Argo"),
                    new Trailer("2", "Gravity"),
                    new Movie("3", "The Ring", "R"),
                    new Movie("4", "Brazil", "R"),
                    new TVShow("5", "Simpsons", 1, 2));
        }
    }

    public static class RootQuery2 {
        @GraphQLQuery
        public List<Content> contents() {
            return Arrays.asList(new Trailer2("1", "Argo"),
                    new Trailer2("2", "Gravity"),
                    new Movie2("3", "The Ring", "R"),
                    new Movie2("4", "Brazil", "R"),
                    new TVShow2("5", "Simpsons", 1, 2));
        }
    }

    @GraphQLInterface(name = "Content", implementationAutoDiscovery = true)
    public interface Content {
        @GraphQLQuery
        String id();

        @GraphQLQuery
        String title();
    }

    public static class ContentBase implements Content {
        private final String id;
        private final String title;
        ContentBase(String id, String title) {
            this.title = title;
            this.id = id;
        }
        @Override
        @GraphQLQuery
        public String id() {return id;}

        @Override
        @GraphQLQuery
        public String title() {return title;}
    }

    public static class Movie extends ContentBase {
        private final String rating;
        Movie(String id, String title, String rating) {
            super(id, title);
            this.rating = rating;
        }

        @GraphQLQuery
        public String rating() {return rating;}
    }

    public static class Trailer extends ContentBase {
        Trailer(String id, String title) {
            super(id, title);
        }
    }

    public static class TVShow extends ContentBase {
        private final Integer seasonNumber;
        private final Integer episodeNumber;
        TVShow(String id, String title, Integer seasonNumber, Integer episodeNumber) {
            super(id, title);
            this.seasonNumber = seasonNumber;
            this.episodeNumber = episodeNumber;
        }

        @GraphQLQuery
        public Integer seasonNumber() {return seasonNumber;}

        @GraphQLQuery
        public Integer episodeNumber() {return episodeNumber;}
    }

    public static class Movie2 implements Content {
        private final String id;
        private final String title;
        private final String rating;
        Movie2(String id, String title, String rating) {
            this.id = id;
            this.title = title;
            this.rating = rating;
        }

        @Override
        @GraphQLQuery
        public String id() {return id;}

        @Override
        @GraphQLQuery
        public String title() {return title;}

        @GraphQLQuery
        public String rating() {return rating;}
    }

    public static class Trailer2 implements Content {
        private final String id;
        private final String title;
        Trailer2(String id, String title) {
            this.id = id;
            this.title = title;
        }
        @Override
        @GraphQLQuery
        public String id() {return id;}

        @Override
        @GraphQLQuery
        public String title() {return title;}
    }

    public static class TVShow2 implements Content {
        private final Integer seasonNumber;
        private final Integer episodeNumber;
        private final String id;
        private final String title;
        TVShow2(String id, String title, Integer seasonNumber, Integer episodeNumber) {
            this.id = id;
            this.title = title;
            this.seasonNumber = seasonNumber;
            this.episodeNumber = episodeNumber;
        }

        @Override
        @GraphQLQuery
        public String id() {return id;}

        @Override
        @GraphQLQuery
        public String title() {return title;}

        @GraphQLQuery
        public Integer seasonNumber() {return seasonNumber;}

        @GraphQLQuery
        public Integer episodeNumber() {return episodeNumber;}
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

        private final T item;

        SessionRepo(T item) {
            this.item = item;
        }

        @GraphQLQuery(name = "item")
        @SuppressWarnings("WeakerAccess") //must stay public
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
        public GraphQLObjectType getType(TypeResolutionEnvironment env) {
            String typeName = "SessionRepo_" + ((SessionRepo) env.getObject()).getStoredItem().getClass().getSimpleName();
            return (GraphQLObjectType) env.getSchema().getType(typeName);
        }
    }
}
