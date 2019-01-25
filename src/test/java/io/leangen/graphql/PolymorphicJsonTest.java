package io.leangen.graphql;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.Argument;
import io.leangen.graphql.annotations.InputField;
import io.leangen.graphql.annotations.Query;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class PolymorphicJsonTest {

    @Parameterized.Parameter
    public ValueMapperFactory valueMapperFactory;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Object[] data() {
        return new Object[] { new JacksonValueMapperFactory(), new GsonValueMapperFactory() };
    }
    
    @Test
    public void testPolymorphicInput() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withValueMapperFactory(valueMapperFactory)
                .withAbstractInputTypeResolution()
                .withOperationsFromSingleton(new Operations())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{" +
                "test (container: {" +
                "       item: \"yay\"," +
                "       _type_: Child}) {" +
                "   item}}");
        assertNoErrors(result);
        assertValueAtPathEquals("yayChild", result, "test.item");
    }

    @Test
    public void testExplicitDeserializableType() {
        //Only test with Jackson as the feature is Jackson specific
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .generate();
        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{ item (in: { item: {}})}");
        assertTrue(result.getErrors().toString(), result.getErrors().isEmpty());
        assertValueAtPathEquals("Concrete", result, "item");
    }

    @Test
    public void testUnambiguousAbstractType() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new VehicleService())
                .withAbstractInputTypeResolution()
                .withValueMapperFactory(valueMapperFactory)
                .generate();
        assertNull(((GraphQLInputObjectType) schema.getType("VehicleInput")).getFieldDefinition("_type_"));
        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{ vehicle(in: {mode: \"flying\"}) {mode}}");
        assertTrue(result.getErrors().toString(), result.getErrors().isEmpty());
        assertValueAtPathEquals("flying", result, "vehicle.mode");
    }

    public static abstract class Vehicle {

        String mode;

        public abstract String getMode();

        public abstract void setMode(String mode);
    }

    public static class Plane extends Vehicle {

        @Override
        public String getMode() {
            return mode;
        }

        @Override
        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class VehicleService {

        @Query
        public Vehicle vehicle(Vehicle in) {
            return in;
        }
    }

    public static abstract class Parent<T> {
        String item;
        
        @Query(value = "item")
        @InputField(name = "item")
        public abstract T getItem();
        public abstract void setItem(T item);
    }

    public static class Child extends Parent<String> {

        @Override
        @Query(value = "item")
        public String getItem() {
            return item + getClass().getSimpleName();
        }

        public void setItem(String item) {
            this.item = item;
        }
    }

    public static class ChildTwo extends Parent<String> {

        @Override
        @Query(value = "item")
        public String getItem() {
            return item + getClass().getSimpleName();
        }

        public void setItem(String item) {
            this.item = item;
        }
    }

    public static class Operations {

        @Query
        public Parent<String> test(@Argument(value = "container") Parent<String> container) {
            return container;
        }
    }

    public static class Service {

        @Query
        public String item(Wrapper in) {
            return in.item.getItem();
        }
    }

    public static class Wrapper {
        @JsonDeserialize(as = Concrete.class)
        Abstract<String> item;
    }

    public interface Abstract<T> {
        T getItem();
    }

    public static class Concrete<T> implements Abstract<T> {

        @SuppressWarnings("unchecked")
        @Override
        public T getItem() {
            return (T) getClass().getSimpleName();
        }
    }
}
