package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.generator.mapping.ConverterRegistry;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.common.CollectionOutputConverter;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.generator.mapping.common.OptionalAdapter;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
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
        assertNoErrors(result);
        assertValueAtPathEquals("xyz", result, "echo.name");
        assertValueAtPathEquals("test1", result, "echo.stream.0.0");
        assertValueAtPathEquals("test2", result, "echo.stream.0.1");
    }

    @Test
    public void testIterableConversion() {
        GraphQL api = getApi();

        ExecutionResult result = api.execute("{echo(in:{name: \"xyz\", iterable: [[[\"test1\", \"test2\"]]]}) {name, iterable}}");
        assertNoErrors(result);
        assertValueAtPathEquals("xyz", result, "echo.name");
        assertValueAtPathEquals("test1", result, "echo.iterable.0.0.0");
        assertValueAtPathEquals("test2", result, "echo.iterable.0.0.1");
    }

    @Test
    public void testMapConversion() {
        GraphQL api = getApi();

        ExecutionResult result = api.execute("{echo(in:{name: \"xyz\", mapList: [[{key: \"test1\", value:1}, {key: \"test2\", value:2}]]}) {name, mapList {key, value}}}");
        assertNoErrors(result);
        assertValueAtPathEquals("xyz", result, "echo.name");
        assertValueAtPathEquals("test1", result, "echo.mapList.0.0.key");
        assertValueAtPathEquals(1, result, "echo.mapList.0.0.value");
        assertValueAtPathEquals("test2", result, "echo.mapList.0.1.key");
        assertValueAtPathEquals(2, result, "echo.mapList.0.1.value");
    }

    @Test
    public void testByteArrayConversion() {
        GraphQL api = getApi();

        ExecutionResult result = api.execute("{echo(in:{binaries: [\"Y2F0cyBhcmUgY3VkZGx5\", \"YnVubmllcyBhcmUgY3VkZGx5IHRvbw==\"]}) {binaries}}");
        assertNoErrors(result);
        assertValueAtPathEquals("Y2F0cyBhcmUgY3VkZGx5", result, "echo.binaries.0");
        assertValueAtPathEquals("YnVubmllcyBhcmUgY3VkZGx5IHRvbw==", result, "echo.binaries.1");
    }

    @Test
    public void testFutureConversion() {
        GraphQL api = getApi();

        ExecutionResult result = api.execute("{echo(in:{future: \"xyz\"}) {future}}");
        assertNoErrors(result);
        assertValueAtPathEquals("xyz", result, "echo.future");
    }

    /**
     * Due to the lack of support for {@code AnnotatedType} in <i>all</i> JSON libraries for Java,
     * {@link ElementType#TYPE_USE} annotations on input field types or nested operation argument types are lost.
     * Thus, such annotations can only safely be used on top-level argument or output types.
     */
    @Test
    public void testIdConversion() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withValueMapperFactory(valueMapperFactory)
                .withOperationsFromSingleton(new IdService())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();

        ExecutionResult result = exe.execute("{echo(id: \"{\\\"key\\\": {\\\"some\\\": \\\"value\\\"}}\") {key}}");
        assertNoErrors(result);

        result = exe.execute("{other(id: \"{\\\"value\\\": \\\"something\\\"}\")}");
        assertNoErrors(result);
    }

    @Test
    public void testConverterOptimization() {
        OutputConverter collectionConverter = new CollectionOutputConverter();
        OutputConverter optionalAdapter = new OptionalAdapter();
        OutputConverter mapToListAdapter = new MapToListTypeAdapter();
        ConverterRegistry registry = new ConverterRegistry(Collections.emptyList(), Arrays.asList(
                collectionConverter, optionalAdapter, mapToListAdapter));

        AnnotatedType string = new TypeToken<String>(){}.getAnnotatedType();
        AnnotatedType listOfStrings = new TypeToken<List<String>>(){}.getAnnotatedType();
        AnnotatedType listOfOptionals = new TypeToken<List<Optional<String>>>(){}.getAnnotatedType();
        AnnotatedType mapOfOptionals = new TypeToken<Map<String, Optional<String>>>(){}.getAnnotatedType();

        List<OutputConverter> optimized = registry.optimize(Collections.singletonList(string)).getOutputConverters();
        assertTrue(optimized.isEmpty());

        optimized = registry.optimize(Collections.singletonList(listOfStrings)).getOutputConverters();
        assertTrue(optimized.isEmpty());

        optimized = registry.optimize(Collections.singletonList(listOfOptionals)).getOutputConverters();
        assertEquals(2, optimized.size());
        assertTrue(optimized.contains(collectionConverter));
        assertTrue(optimized.contains(optionalAdapter));

        optimized = registry.optimize(Collections.singletonList(mapOfOptionals)).getOutputConverters();
        assertEquals(2, optimized.size());
        assertTrue(optimized.contains(mapToListAdapter));
        assertTrue(optimized.contains(optionalAdapter));

        ConverterRegistry optimizedRegistry = registry.optimize(Arrays.asList(listOfOptionals, mapOfOptionals));
        assertSame(registry, optimizedRegistry);
    }

    private GraphQL getApi() {
        return GraphQL.newGraphQL(
                new TestSchemaGenerator()
                        .withValueMapperFactory(valueMapperFactory)
                        .withTypeAdapters(new MapToListTypeAdapter())
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
        private List<byte[]> binaries;
        private Stream<Iterable<List<Optional<String>>>> iterable;
        private CompletableFuture<Optional<String>> future;

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

        public List<byte[]> getBinaries() {
            return binaries;
        }

        public void setBinaries(List<byte[]> binaries) {
            this.binaries = binaries;
        }

        public Stream<Iterable<List<Optional<String>>>> getIterable() {
            List<Optional<String>> inner = new ArrayList<>();
            iterable.forEach(iter -> {
                for (List<Optional<String>> item : iter) {
                    inner.addAll(item);
                }
            });
            return Stream.of(new IterX<>(Collections.singletonList(inner)));
        }

        public void setIterable(Stream<Iterable<List<Optional<String>>>> iterable) {
            this.iterable = iterable;
        }

        public CompletableFuture<Optional<String>> getFuture() {
            return future;
        }

        public void setFuture(CompletableFuture<Optional<String>> future) {
            this.future = future;
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

    //An Iterable that is not a Collection
    private static class IterX<T> implements Iterable<T> {

        private final List<T> source;

        IterX(List<T> source) {
            this.source = source;
        }

        @Override
        public Iterator<T> iterator() {
            return source.iterator();
        }
    }
}
