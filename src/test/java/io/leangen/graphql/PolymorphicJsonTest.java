package io.leangen.graphql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;

import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
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
    public void test() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withValueMapperFactory(valueMapperFactory)
                .withOperationsFromSingleton(new Operations())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{" +
                "test (container: {" +
                "       item: \"yay\"," +
                "       _type_:\"Child\"}) {" +
                "   _type_," +
                "   item}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("Child", result, "test._type_");
        assertValueAtPathEquals("yay", result, "test.item");
    }
    
    public static abstract class Parent<T> {
        String item;
        
        @GraphQLQuery(name = "item")
        @GraphQLInputField(name = "item")
        public abstract T getItem();
        public abstract void setItem(T item);
    }

    public static class Child extends Parent<String> {

        public Child() {
        }

        @Override
        @GraphQLQuery(name = "item")
        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item;
        }
    }

    public static class Operations {

        @GraphQLQuery
        public Parent<String> test(@GraphQLArgument(name = "container") Parent<String> container) {
            return container;
        }
    }
}
