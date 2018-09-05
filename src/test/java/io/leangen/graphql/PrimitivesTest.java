package io.leangen.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import org.junit.Test;

import static io.leangen.graphql.support.GraphQLTypeAssertions.assertNonNull;
import static org.junit.Assert.assertSame;

public class PrimitivesTest {

    @Test
    public void nonNullIntTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new PrimitiveService())
                .generate();
        GraphQLObjectType query = schema.getQueryType();
        GraphQLFieldDefinition field;

        field = query.getFieldDefinition("primitive");
        assertNonNull(field.getType(), Scalars.GraphQLInt);
        assertNonNull(field.getArgument("in").getType(), Scalars.GraphQLInt);

        field = query.getFieldDefinition("primitiveWithDefault");
        assertNonNull(field.getType(), Scalars.GraphQLInt);
        assertSame(field.getArgument("in").getType(), Scalars.GraphQLInt);

        field = query.getFieldDefinition("nonNullPrimitive");
        assertNonNull(field.getType(), Scalars.GraphQLInt);
        assertNonNull(field.getArgument("in").getType(), Scalars.GraphQLInt);

        field = query.getFieldDefinition("relayIdPrimitive");
        assertSame(field.getType(), io.leangen.graphql.util.Scalars.RelayId);
        assertSame(field.getArgument(GraphQLId.RELAY_ID_FIELD_NAME).getType(), io.leangen.graphql.util.Scalars.RelayId);

        field = query.getFieldDefinition("nonNullInteger");
        assertNonNull(field.getType(), Scalars.GraphQLInt);
        assertNonNull(field.getArgument("in").getType(), Scalars.GraphQLInt);

        field = query.getFieldDefinition("integer");
        assertSame(field.getType(), Scalars.GraphQLInt);
        assertSame(field.getArgument("in").getType(), Scalars.GraphQLInt);
    }

    @Test
    public void nonNullBooleanVoidTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new BooleanVoidService())
                .generate();
        GraphQLObjectType query = schema.getQueryType();
        GraphQLFieldDefinition field;

        field = query.getFieldDefinition("primitiveVoid");
        assertNonNull(field.getType(), Scalars.GraphQLBoolean);

        field = query.getFieldDefinition("objVoid");
        assertSame(field.getType(), Scalars.GraphQLBoolean);

        field = query.getFieldDefinition("primitiveBoolean");
        assertNonNull(field.getType(), Scalars.GraphQLBoolean);
        assertNonNull(field.getArgument("in").getType(), Scalars.GraphQLBoolean);

        field = query.getFieldDefinition("objBoolean");
        assertSame(field.getType(), Scalars.GraphQLBoolean);
        assertSame(field.getArgument("in").getType(), Scalars.GraphQLBoolean);
    }

    private static class PrimitiveService {

        @GraphQLQuery
        public int primitive(int in) {
            return in;
        }

        @GraphQLQuery
        public int primitiveWithDefault(@GraphQLArgument(name = "in", defaultValue = "3") int in) {
            return in;
        }

        @GraphQLQuery
        public @GraphQLNonNull int nonNullPrimitive(@GraphQLNonNull int in) {
            return in;
        }

        @GraphQLQuery
        public @GraphQLId(relayId = true) int relayIdPrimitive(@GraphQLId(relayId = true) int in) {
            return in;
        }

        @GraphQLQuery
        public Integer integer(Integer in) {
            return in;
        }

        @GraphQLQuery
        public @GraphQLNonNull Integer nonNullInteger(@GraphQLNonNull Integer in) {
            return in;
        }
    }

    private static class BooleanVoidService {

        @GraphQLQuery
        public void primitiveVoid() {}

        @GraphQLQuery
        public Void objVoid() {
            return null;
        }

        @GraphQLQuery
        public boolean primitiveBoolean(boolean in) {
            return in;
        }

        @GraphQLQuery
        public Boolean objBoolean(Boolean in) {
            return in;
        }
    }
}
