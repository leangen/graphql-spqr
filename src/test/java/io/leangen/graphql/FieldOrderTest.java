package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.annotations.types.GraphQLType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FieldOrderTest {

    @Test
    public void test() {
        ExecutionResult result = introspect();

        List<String> characterFields = getTypeFieldsFromIntrospectionResult(result, "Character");
        Assert.assertEquals(Arrays.asList("id", "name", "friends", "appearsIn"), characterFields);

        List<String> humanFields = getTypeFieldsFromIntrospectionResult(result, "Human");
        Assert.assertEquals(Arrays.asList("id", "name", "friends", "appearsIn", "starships", "totalCredits"), humanFields);

        List<String> droidFields = getTypeFieldsFromIntrospectionResult(result, "Droid");
        Assert.assertEquals(Arrays.asList("id", "appearsIn", "friends", "name", "primaryFunction"), droidFields);

        List<String> planetFields = getTypeFieldsFromIntrospectionResult(result, "Planet");
        Assert.assertEquals(Arrays.asList("name", "size", "climate", "coordinates"), planetFields);

        List<String> planetInputFields = getTypeInputFieldsFromIntrospectionResult(result, "PlanetInput");
        Assert.assertEquals(Arrays.asList("climate", "coordinates", "name", "size"), planetInputFields);

        List<String> episodeFields = getTypeFieldsFromIntrospectionResult(result, "Episode");
        Assert.assertEquals(Arrays.asList("episodeID", "title", "director", "openingCrawl"), episodeFields);

        List<String> episodeInputFields = getTypeInputFieldsFromIntrospectionResult(result, "EpisodeInput");
        Assert.assertEquals(Arrays.asList("episodeID", "title", "director", "openingCrawl"), episodeInputFields);
    }

    public static class Query {
        @GraphQLQuery
        public Character hero() { return null; }

        @GraphQLQuery
        public Planet planet(Planet planet) { return null; }

        @GraphQLQuery
        public Episode episode(Episode episode) { return null; }
    }

    @GraphQLInterface(name = "Character", implementationAutoDiscovery = true, fieldOrder = {"id", "name", "friends", "appearsIn"})
    public interface Character {
        String getId();
        String getName();
        String getFriends();
        String getAppearsIn();
    }

    @GraphQLType(name = "Human", fieldOrder = {"id", "name", "friends", "appearsIn", "starships", "totalCredits"})
    public static class Human implements Character {
        public String getId() { return null; }
        public String getName() { return null; }
        public String getFriends() { return null; }
        public String getAppearsIn() { return null; }
        public String getStarships() { return null; }
        public String getTotalCredits() { return null; }
    }

    @GraphQLType(name = "Droid", fieldOrder = {"id" /*rest alphabetically*/})
    public static class Droid implements Character {
        public String getId() { return null; }
        public String getName() { return null; }
        public String getFriends() { return null; }
        public String getAppearsIn() { return null; }
        public String getPrimaryFunction() { return null; }
    }

    @GraphQLType(fieldOrder = {"name", "size"}, inputFieldOrder = "climate")
    public static class Planet {
        public String name;
        public String climate;
        public String coordinates;
        public String size;

        public static String shouldBeOmitted;
    }

    @GraphQLType(fieldOrder = {"episodeID", "title"})
    public static class Episode {
        public String title;
        public String episodeID;
        public String openingCrawl;
        public String director;
    }

    private static ExecutionResult introspect() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withBasePackages(FieldOrderTest.class.getPackage().getName())
                .withOperationsFromSingletons(new Query())
                .generate();
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        return graphQL.execute(IntrospectionQuery.INTROSPECTION_QUERY);
    }

    private static List<String> getTypeFieldsFromIntrospectionResult(ExecutionResult result, String typeName) {
        return getTypeFieldsFromIntrospectionResult(result, typeName, "fields");
    }

    private static List<String> getTypeInputFieldsFromIntrospectionResult(ExecutionResult result, String typeName) {
        return getTypeFieldsFromIntrospectionResult(result, typeName, "inputFields");
    }

    @SuppressWarnings("unchecked")
    private static List<String> getTypeFieldsFromIntrospectionResult(ExecutionResult result, String typeName, String fieldType) {
        Map<String, Object> data = result.getData();
        Map<String, Object> __schema = (Map<String, Object>) data.get("__schema");
        List<Map<String, Object>> types = (List<Map<String, Object>>) __schema.get("types");
        Map<String, Object> type = types.stream()
                .filter(t -> Objects.equals(t.get("name"), typeName))
                .findFirst()
                .get();
        List<Map<String, Object>> fields = (List<Map<String, Object>>)type.get(fieldType);
        return fields.stream()
                .map(field -> (String) field.get("name"))
                .collect(Collectors.toList());
    }
}
