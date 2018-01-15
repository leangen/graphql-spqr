package io.leangen.graphql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.ElementType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.generator.mapping.common.StreamToCollectionTypeAdapter;
import io.leangen.graphql.generator.mapping.strategy.ObjectScalarStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;

import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests whether various input/output converters are doing their job
 */
@RunWith(Parameterized.class)
public class ConversionTest {

    @Parameterized.Parameter
    public ValueMapperFactory valueMapperFactory;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Object[] data() {
        return new Object[] { new JacksonValueMapperFactory(), new GsonValueMapperFactory() };
    }

    @Test
    public void testStreamConversion() {
        GraphQL api = getApi();

        ExecutionResult result = api.execute("{echo(in:{name: \"xyz\", stream: [[\"test1\", \"test2\"]]}) {name, stream}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("xyz", result, "echo.name");
        assertValueAtPathEquals("test1", result, "echo.stream.0.0");
        assertValueAtPathEquals("test2", result, "echo.stream.0.1");
    }

    @Test
    public void testMapConversion() {
        GraphQL api = getApi();

        ExecutionResult result = api.execute("{echo(in:{name: \"xyz\", mapList: [[{key: \"test1\", value:1}, {key: \"test2\", value:2}]]}) {name, mapList {key, value}}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("xyz", result, "echo.name");
        assertValueAtPathEquals("test1", result, "echo.mapList.0.0.key");
        assertValueAtPathEquals(1, result, "echo.mapList.0.0.value");
        assertValueAtPathEquals("test2", result, "echo.mapList.0.1.key");
        assertValueAtPathEquals(2, result, "echo.mapList.0.1.value");
    }

    /**
     * Due to the lack of support for {@code AnnotatedType} in <i>all</i> JSON libraries for Java,
     * {@link ElementType#TYPE_USE} annotations on input field types or nested operation argument types are lost.
     * Thus, such annotations can only safely be used on top-level argument or output types.
     */
    @Test
    public void testIdConversion() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withDefaultArgumentInjectors()
                .withValueMapperFactory(valueMapperFactory)
                .withOperationsFromSingleton(new IdService())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();

        ExecutionResult result = exe.execute("{echo(id: \"{\\\"key\\\": {\\\"some\\\": \\\"value\\\"}}\") {key}}");
        assertTrue(result.getErrors().isEmpty());

        result = exe.execute("{other(id: \"{\\\"value\\\": \\\"something\\\"}\")}");
        assertTrue(result.getErrors().isEmpty());
    }

    private GraphQL getApi() {
        return GraphQL.newGraphQL(
                new TestSchemaGenerator()
                        .withValueMapperFactory(valueMapperFactory)
                        .withTypeAdapters(
                                new MapToListTypeAdapter<>(new ObjectScalarStrategy()),
                                new StreamToCollectionTypeAdapter())
                        .withDefaults()
                        .withOperationsFromSingleton(new ComplexService())
                        .generate())
                .build();
    }

    public static class ComplexService {
        @GraphQLQuery(name = "echo")
        public ComplexObject echoArgument(@GraphQLArgument(name = "in") ComplexObject in) {
            return in;
        }
    }

    public static class ComplexObject {

        private String name;
        private Stream<Stream<String>> stream;
        private List<Optional<LinkedHashMap<String, Integer>>> mapList;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Stream<Stream<String>> getStream() {
            return stream;
        }

        public void setStream(Stream<Stream<String>> stream) {
            this.stream = stream;
        }

        public List<Optional<LinkedHashMap<String, Integer>>> getMapList() {
            return mapList;
        }

        public void setMapList(List<Optional<LinkedHashMap<String, Integer>>> mapList) {
            this.mapList = mapList;
        }
    }

    public static class IdService {
        @GraphQLQuery
        public NestedId echo(@GraphQLId(relayId = true) NestedId id) {
            return id;
        }

        @GraphQLQuery
        public AnnotatedId other(@GraphQLArgument(name = "id") AnnotatedId id) {
            return id;
        }
    }

    public static class NestedId {

        private Map<String, String> key;

        public @GraphQLId(relayId = true) Map<String, String> getKey() {
            return key;
        }

        public void setKey(Map<String, String> key) {
            this.key = key;
        }
    }

    @GraphQLId(relayId = true)
    public static class AnnotatedId {
        public String value;
    }
}
