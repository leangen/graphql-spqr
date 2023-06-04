package io.leangen.graphql;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.metadata.strategy.DefaultInclusionStrategy;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import org.junit.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class InclusionStrategyTest {

    @Test
    public void testNoExclusion() {
        GraphQLSchema schema = schema(new DefaultInclusionStrategy());
        assertEquals(3, schema.getQueryType().getFieldDefinitions().size());
    }

    @Test
    public void testStaticExclusion() {
        GraphQLSchema schema = schema(new DefaultInclusionStrategy().excludeStaticMembers());
        List<GraphQLFieldDefinition> fields = schema.getQueryType().getFieldDefinitions();
        assertEquals(1, fields.size());
        assertEquals("query", fields.get(0).getName());
    }

    @Test
    public void testCustomExclusion() {
        GraphQLSchema schema = schema(new NameBasedInclusionStrategy());
        List<GraphQLFieldDefinition> fields = schema.getQueryType().getFieldDefinitions();
        assertEquals(1, fields.size());
        assertEquals("query", fields.get(0).getName());
    }

    private static GraphQLSchema schema(InclusionStrategy inclusionStrategy) {
        ExecutableSchema executableSchema = new TestSchemaGenerator()
                .withInclusionStrategy(inclusionStrategy)
                .withOperationsFromSingleton(new TestService(), new PublicResolverBuilder())
                .generateExecutable();
        return executableSchema.getSchema();
    }

    private static class NameBasedInclusionStrategy extends DefaultInclusionStrategy {
        @Override
        public boolean includeOperation(List<AnnotatedElement> elements, AnnotatedType declaringType) {
            return isAnyElementNamed(elements, "query") && super.includeOperation(elements, declaringType);
        }

        private static boolean isAnyElementNamed(List<AnnotatedElement> elements, String name) {
            return elements.stream().anyMatch(e -> ((Member) e).getName().equals(name));
        }
    }

    @SuppressWarnings("unused")
    private static class TestService {

        public static String staticField;

        public static String staticMethod() {
            return null;
        }

        public String query() {
            return null;
        }
    }
}
