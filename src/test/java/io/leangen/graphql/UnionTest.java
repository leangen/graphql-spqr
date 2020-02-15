package io.leangen.graphql;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLUnion;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.generator.mapping.strategy.DefaultImplementationDiscoveryStrategy;
import io.leangen.graphql.util.GraphQLUtils;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static io.leangen.graphql.support.GraphQLTypeAssertions.assertListOf;
import static io.leangen.graphql.support.GraphQLTypeAssertions.assertMapOf;
import static io.leangen.graphql.support.GraphQLTypeAssertions.assertNonNull;
import static io.leangen.graphql.support.GraphQLTypeAssertions.assertUnionOf;
import static org.junit.Assert.assertEquals;

/**
 * @author Bojan Tomic (kaqqao)
 */
@SuppressWarnings("WeakerAccess")
public class UnionTest {

    @Test
    public void testInlineUnion() {
        InlineUnionService unionService = new InlineUnionService();

        GraphQLSchema schema = new TestSchemaGenerator()
                .withTypeAdapters(new MapToListTypeAdapter())
                .withOperationsFromSingleton(unionService)
                .generate();

        GraphQLOutputType fieldType = schema.getQueryType().getFieldDefinition("union").getType();
        assertNonNull(fieldType, GraphQLList.class);
        GraphQLType list = ((graphql.schema.GraphQLNonNull) fieldType).getWrappedType();
        assertListOf(list, GraphQLList.class);
        GraphQLType map = ((GraphQLList) list).getWrappedType();
        assertMapOf(map, GraphQLUnionType.class, GraphQLUnionType.class);
        GraphQLObjectType entry = (GraphQLObjectType) GraphQLUtils.unwrap(map);
        GraphQLUnionType key = (GraphQLUnionType) entry.getFieldDefinition("key").getType();
        GraphQLNamedOutputType value = (GraphQLNamedOutputType) entry.getFieldDefinition("value").getType();
        assertEquals("Simple_One_Two", key.getName());
        assertEquals("nice", key.getDescription());
        assertEquals(value.getName(), "Education_Street");
        assertUnionOf(key, schema.getType("SimpleOne"), schema.getType("SimpleTwo"));
        assertUnionOf(value, schema.getType("Education"), schema.getType("Street"));
    }

    @Test
    public void testExplicitUnionClass() {
        ExplicitUnionClassService unionService = new ExplicitUnionClassService();

        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(unionService)
                .generate();

        GraphQLUnionType union = (GraphQLUnionType) schema.getQueryType().getFieldDefinition("union").getType();
        assertUnionOf(union, schema.getType("C1"), schema.getType("C2"));
        assertEquals("Strong_union", union.getName());
        assertEquals("This union is strong!", union.getDescription());
    }

    @Test
    public void testExplicitUnionInterface() {
        ExplicitUnionInterfaceService unionService = new ExplicitUnionInterfaceService();

        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(unionService)
                .generate();

        GraphQLUnionType union = (GraphQLUnionType) schema.getQueryType().getFieldDefinition("union").getType();
        assertUnionOf(union, schema.getType("I1"), schema.getType("I2"));
        assertEquals("Strong_union", union.getName());
        assertEquals("This union is strong!", union.getDescription());
    }

    @Test
    public void testRegisteredImplementationsUnionInterface() {
        AdditionalUnionInterfaceService unionService = new AdditionalUnionInterfaceService();

        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(unionService)
                .withImplementationDiscoveryStrategy(new DefaultImplementationDiscoveryStrategy()
                        .withAdditionalImplementations(I1.class, I2.class))
                .generate();

        GraphQLUnionType union = (GraphQLUnionType) schema.getQueryType().getFieldDefinition("union").getType();
        assertUnionOf(union, schema.getType("I1"), schema.getType("I2"));
        assertEquals("Strong_union", union.getName());
        assertEquals("This union is strong!", union.getDescription());
    }

    @Test
    public void testAutoDiscoveredUnionInterface() {
        AutoDiscoveredUnionService unionService = new AutoDiscoveredUnionService();

        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(unionService)
                .generate();

        GraphQLUnionType union = (GraphQLUnionType) schema.getQueryType().getFieldDefinition("union").getType();
        assertUnionOf(union, schema.getType("A1"), schema.getType("A2"));
        assertEquals("Strong_union", union.getName());
        assertEquals("This union is strong!", union.getDescription());
    }

    private class InlineUnionService {
        @GraphQLQuery(name = "union")
        public List<Map<@io.leangen.graphql.annotations.GraphQLUnion(name = "Simple_One_Two") ? super SimpleOne, @io.leangen.graphql.annotations.GraphQLUnion(name = "Education_Street") Street>> union(@GraphQLArgument(name = "id") int id) {
            return null;
        }

        @GraphQLQuery(name = "union")
        public @GraphQLNonNull List<Map<@io.leangen.graphql.annotations.GraphQLUnion(name = "Simple_One_Two") ? extends SimpleTwo, @io.leangen.graphql.annotations.GraphQLUnion(name = "Education_Street") Education>> union2(@GraphQLArgument(name = "id") int id) {
            return null;
        }

        @GraphQLQuery(name = "union")
        public List<Map<@io.leangen.graphql.annotations.GraphQLUnion(name = "Simple_One_Two", description = "nice") SimpleTwo, @io.leangen.graphql.annotations.GraphQLUnion(name = "Education_Street") Street>> union3(@GraphQLArgument(name = "id") int id) {
            return null;
        }
    }

    private class ExplicitUnionClassService {
        @GraphQLQuery(name = "union")
        public UC union(@GraphQLArgument(name = "id") int id) {
            return null;
        }
    }

    private class ExplicitUnionInterfaceService {
        @GraphQLQuery(name = "union")
        public UI union(@GraphQLArgument(name = "id") int id) {
            return null;
        }
    }

    private class AdditionalUnionInterfaceService {
        @GraphQLQuery(name = "union")
        public UE union(@GraphQLArgument(name = "id") int id) {
            return null;
        }
    }

    private class AutoDiscoveredUnionService {
        @GraphQLQuery(name = "union")
        public UA union(@GraphQLArgument(name = "id") int id) {
            return null;
        }
    }

    @GraphQLUnion(name = "Strong_union", description = "This union is strong!", possibleTypes = {C2.class, C1.class})
    public static class UC {}

    public static class C1 extends UC {}
    public static class C2 extends UC {}

    @GraphQLUnion(name = "Strong_union", description = "This union is strong!", possibleTypes = {I2.class, I1.class})
    public interface UI {}

    @GraphQLUnion(name = "Strong_union", description = "This union is strong!")
    public interface UE {}

    public static class I1 implements UI, UE {}
    public static class I2 implements UI, UE {}

    @GraphQLUnion(name = "Strong_union", description = "This union is strong!", possibleTypeAutoDiscovery = true, scanPackages = "io.leangen")
    public interface UA {}

    public static class A1 implements UA {}
    public static class A2 implements UA {}

    @io.leangen.graphql.annotations.types.GraphQLType(name = "SimpleOne")
    public static class SimpleOne {

        @GraphQLQuery(name = "one")
        public String getOne() {
            return "one";
        }
    }

    @io.leangen.graphql.annotations.types.GraphQLType(name = "SimpleTwo")
    public static class SimpleTwo {

        @GraphQLQuery(name = "two")
        public String getTwo() {
            return "two";
        }
    }
}
