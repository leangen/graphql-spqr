package io.leangen.graphql;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.types.GraphQLUnion;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.util.GraphQLUtils;
import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Type;
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
                .withTypeAdapters(new MapToListTypeAdapter<>())
                .withOperationsFromSingleton(unionService)
                .generate();

        GraphQLOutputType fieldType = schema.getQueryType().getFieldDefinition("union").getType();
        assertNonNull(fieldType, GraphQLList.class);
        GraphQLType list = ((graphql.schema.GraphQLNonNull) fieldType).getWrappedType();
        assertListOf(list, GraphQLList.class);
        GraphQLType map = ((GraphQLList) list).getWrappedType();
        assertMapOf(map, GraphQLUnionType.class, GraphQLUnionType.class);
        GraphQLObjectType entry = (GraphQLObjectType) GraphQLUtils.unwrap(map);
        GraphQLOutputType key = entry.getFieldDefinition("key").getType();
        GraphQLOutputType value = entry.getFieldDefinition("value").getType();
        assertEquals("Simple_One_Two", key.getName());
        assertEquals("nice", ((GraphQLUnionType) key).getDescription());
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

        GraphQLOutputType union = schema.getQueryType().getFieldDefinition("union").getType();
        assertUnionOf(union, schema.getType("C1"), schema.getType("C2"));
        assertEquals("Strong_union", union.getName());
        assertEquals("This union is strong!", ((GraphQLUnionType) union).getDescription());
    }

    @Test
    public void testExplicitUnionInterface() {
        ExplicitUnionInterfaceService unionService = new ExplicitUnionInterfaceService();

        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(unionService)
                .generate();

        GraphQLOutputType union = schema.getQueryType().getFieldDefinition("union").getType();
        assertUnionOf(union, schema.getType("I1"), schema.getType("I2"));
        assertEquals("Strong_union", union.getName());
        assertEquals("This union is strong!", ((GraphQLUnionType) union).getDescription());
    }

    @Test
    public void testAutoDiscoveredUnionInterface() {
        AutoDiscoveredUnionService unionService = new AutoDiscoveredUnionService();

        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(unionService)
                .generate();

        GraphQLOutputType union = schema.getQueryType().getFieldDefinition("union").getType();
        assertUnionOf(union, schema.getType("A1"), schema.getType("A2"));
        assertEquals("Strong_union", union.getName());
        assertEquals("This union is strong!", ((GraphQLUnionType) union).getDescription());
    }

    private class InlineUnionService {
        @Query(value = "union")
        public List<Map<@io.leangen.graphql.annotations.GraphQLUnion(name = "Simple_One_Two") ? super SimpleOne, @io.leangen.graphql.annotations.GraphQLUnion(name = "Education_Street") Street>> union(@Argument(value = "id") int id) {
            return null;
        }

        @Query(value = "union")
        public @GraphQLNonNull List<Map<@io.leangen.graphql.annotations.GraphQLUnion(name = "Simple_One_Two") ? extends SimpleTwo, @io.leangen.graphql.annotations.GraphQLUnion(name = "Education_Street") Education>> union2(@Argument(value = "id") int id) {
            return null;
        }

        @Query(value = "union")
        public List<Map<@io.leangen.graphql.annotations.GraphQLUnion(name = "Simple_One_Two", description = "nice") SimpleTwo, @io.leangen.graphql.annotations.GraphQLUnion(name = "Education_Street") Street>> union3(@Argument(value = "id") int id) {
            return null;
        }
    }

    private class ExplicitUnionClassService {
        @Query(value = "union")
        public UC union(@Argument(value = "id") int id) {
            return null;
        }
    }

    private class ExplicitUnionInterfaceService {
        @Query(value = "union")
        public UI union(@Argument(value = "id") int id) {
            return null;
        }
    }

    private class AutoDiscoveredUnionService {
        @Query(value = "union")
        public UA union(@Argument(value = "id") int id) {
            return null;
        }
    }

    @GraphQLUnion(name = "Strong_union", description = "This union is strong!", possibleTypes = {C1.class, C2.class})
    public static class UC {}

    public static class C1 extends UC {}
    public static class C2 extends UC {}

    @GraphQLUnion(name = "Strong_union", description = "This union is strong!", possibleTypes = {I1.class, I2.class})
    public interface UI {}

    public static class I1 implements UI {}
    public static class I2 implements UI {}

    @GraphQLUnion(name = "Strong_union", description = "This union is strong!", possibleTypeAutoDiscovery = true, scanPackages = "io.leangen")
    public interface UA {}

    public static class A1 implements UA {}
    public static class A2 implements UA {}

    @Type(value = "SimpleOne")
    public static class SimpleOne {

        @Query(value = "one")
        public String getOne() {
            return "one";
        }
    }

    @Type(value = "SimpleTwo")
    public static class SimpleTwo {

        @Query(value = "two")
        public String getTwo() {
            return "two";
        }
    }
}
