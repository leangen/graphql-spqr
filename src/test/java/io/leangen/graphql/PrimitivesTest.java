package io.leangen.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
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

    private static class PrimitiveService {

        @GraphQLQuery
        public int primitive(int in) {
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
}
