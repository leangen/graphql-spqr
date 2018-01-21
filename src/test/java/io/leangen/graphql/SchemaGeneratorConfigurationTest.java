package io.leangen.graphql;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.common.ByteArrayToBase64Adapter;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.generator.mapping.common.NonNullMapper;
import io.leangen.graphql.generator.mapping.common.ObjectTypeMapper;
import io.leangen.graphql.generator.mapping.common.ScalarMapper;
import io.leangen.graphql.generator.mapping.common.StreamToCollectionTypeAdapter;
import io.leangen.graphql.generator.mapping.strategy.DefaultScalarStrategy;
import io.leangen.graphql.generator.mapping.strategy.ScalarMappingStrategy;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class SchemaGeneratorConfigurationTest {

    private static List<TypeMapper> defaultMappers = getDefaultMappers();

    @Test
    public void testExplicitMappersOnly() {
        ByteArrayToBase64Adapter byteMapper = new ByteArrayToBase64Adapter();
        ScalarMapper scalarMapper =  new ScalarMapper();
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withTypeMappers(byteMapper, scalarMapper);
        generator.generate();

        List<TypeMapper> typeMappers = getTypeMappers(generator);
        assertEquals(2, typeMappers.size());
        assertEquals(byteMapper, typeMappers.get(0));
        assertEquals(scalarMapper, typeMappers.get(1));
    }

    @Test
    public void testImplicitDefaultMappersOnly() {
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy());
        generator.generate();

        List<TypeMapper> typeMappers = getTypeMappers(generator);
        assertTrue(typeMappers.size() > 20);
        assertEquals(defaultMappers.size(), typeMappers.size());
        for (int i = 0; i < typeMappers.size(); i++) {
            assertEquals(defaultMappers.get(i).getClass(), typeMappers.get(i).getClass());
        }
    }

    @Test
    public void testModifiedDefaultMappersOnly() {
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withTypeMappers((conf, mappers) -> mappers.drop(NonNullMapper.class));
        generator.generate();

        List<TypeMapper> typeMappers = getTypeMappers(generator);
        assertEquals(defaultMappers.size() - 1, typeMappers.size());
        assertTrue(typeMappers.stream().noneMatch(mapper -> mapper instanceof NonNullMapper));

        generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withTypeMappers((conf, mappers) -> mappers.insert(11, new MapToListTypeAdapter<>(conf.scalarMappingStrategy)));
        generator.generate();

        typeMappers = getTypeMappers(generator);
        assertEquals(defaultMappers.size() + 1, typeMappers.size());
        assertTrue(typeMappers.get(11) instanceof MapToListTypeAdapter);
    }

    @Test
    public void testExplicitPlusDefaultMappers() {
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withTypeMappers(new MapToListTypeAdapter(new DefaultScalarStrategy()))
                .withDefaultMappers();
        generator.generate();

        List<TypeMapper> typeMappers = getTypeMappers(generator);
        assertEquals(defaultMappers.size() + 1, typeMappers.size());
        assertTrue(typeMappers.get(0) instanceof MapToListTypeAdapter);
    }

    @Test
    public void testInsertedMappers() {
        ScalarMappingStrategy def = new DefaultScalarStrategy();
        TypeMapper m1 = new MapToListTypeAdapter<>(def);
        TypeMapper m2 = new MapToListTypeAdapter<>(def);
        TypeMapper m11 = new MapToListTypeAdapter<>(def);
        TypeMapper m21 = new MapToListTypeAdapter<>(def);
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withTypeMappers((config, defaults) -> defaults
                        .insertAfter(ScalarMapper.class, m1, m11)
                        .insertBefore(ObjectTypeMapper.class, m2, m21)
                        .drop(StreamToCollectionTypeAdapter.class));
        generator.generate();

        List<TypeMapper> typeMappers = getTypeMappers(generator);
        int m1Ix = typeMappers.indexOf(m1);
        int m2Ix = typeMappers.indexOf(m2);
        assertEquals(defaultMappers.size() + 3, typeMappers.size());
        assertTrue(typeMappers.get(m1Ix - 1) instanceof ScalarMapper);
        assertEquals(m11, typeMappers.get(m1Ix + 1));
        assertTrue(typeMappers.get(m2Ix + 2) instanceof ObjectTypeMapper);
        assertEquals(m21, typeMappers.get(m2Ix + 1));
        assertTrue(typeMappers.stream().noneMatch(m -> m instanceof StreamToCollectionTypeAdapter));
    }

    @Test
    public void testExplicitPlusModifiedDefaultMappers() {
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withTypeMappers(new MapToListTypeAdapter(new DefaultScalarStrategy()))
                .withTypeMappers((conf, mappers) -> mappers.drop(NonNullMapper.class));
        generator.generate();

        List<TypeMapper> typeMappers = getTypeMappers(generator);
        assertEquals(defaultMappers.size(), typeMappers.size());
        assertTrue(typeMappers.get(0) instanceof MapToListTypeAdapter);
        assertTrue(typeMappers.stream().noneMatch(mapper -> mapper instanceof NonNullMapper));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoMappers() {
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withTypeMappers((conf, mappers) -> Collections.emptyList());
        generator.generate();
    }

    @Test
    public void testDuplicateDefaults() {
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withDefaultMappers()
                .withDefaultMappers()
                .withTypeMappers((config, defaults) -> defaults)
                .withTypeMappers((config, defaults) -> defaults);
        generator.generate();

        List<TypeMapper> typeMappers = getTypeMappers(generator);
        assertEquals(defaultMappers.size(), typeMappers.size());
    }

    @Test
    public void testExplicitDuplicates() {
        ByteArrayToBase64Adapter byteMapper = new ByteArrayToBase64Adapter();
        ScalarMapper scalarMapper =  new ScalarMapper();
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withTypeMappers(byteMapper, scalarMapper)
                .withTypeMappers(byteMapper, scalarMapper)
                .withTypeMappers(((config, defaults) -> Arrays.asList(byteMapper, scalarMapper)));
        generator.generate();

        List<TypeMapper> typeMappers = getTypeMappers(generator);
        assertEquals(2, typeMappers.size());
        assertEquals(byteMapper, typeMappers.get(0));
        assertEquals(scalarMapper, typeMappers.get(1));
    }

    private static List<TypeMapper> getDefaultMappers() {
        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Dummy())
                .withDefaultMappers();
        generator.generate();

        return getTypeMappers(generator);
    }

    @SuppressWarnings("unchecked")
    private static List<TypeMapper> getTypeMappers(GraphQLSchemaGenerator generator) {
        try {
            Field mappers = GraphQLSchemaGenerator.class.getDeclaredField("typeMappers");
            mappers.setAccessible(true);
            return (List<TypeMapper>) mappers.get(generator);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Dummy {
        @GraphQLQuery
        public byte[] bytes;
    }
}
