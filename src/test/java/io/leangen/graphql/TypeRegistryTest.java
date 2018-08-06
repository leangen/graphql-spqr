package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.union.Union2;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypeRegistryTest {
    
    @Test
    public void referenceReplacementTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{mix(id: \"1\") {... on Street {name}}}");
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void knownTypeTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .generate();

        schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .withAdditionalTypes(schema.getAllTypesAsList())
                .generate();
        
        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{mix(id: \"1\") {... on Street {name}}}");
        assertEquals(0, result.getErrors().size());
    }
    
    public class Service {
        
        @GraphQLQuery(name = "street")
        public Street getStreet() {
            return null;
        }
        
        @GraphQLQuery
        public @GraphQLUnion(name = "mix") Union2<Street, Education> mix(@GraphQLEnvironment ResolutionEnvironment env, @GraphQLId(relayId = true) int id) {
            GraphQLOutputType mixType = env.globalEnvironment.typeRegistry.getOutputTypes("mix").get(0).graphQLType;
            assertTrue(mixType instanceof GraphQLObjectType);
            return null;
        }
    }
}
