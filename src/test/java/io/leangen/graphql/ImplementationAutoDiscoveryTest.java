package io.leangen.graphql;

import org.junit.Test;

import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLInterface;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests whether auto-discovered interface implementation types are correctly registered in the schema
 */
public class ImplementationAutoDiscoveryTest {

    @Test
    public void hiddenTypesTest() {
        GraphQLSchema schema = schemaFor(new FlatService());
        assertNull(schema.getType("One"));
        assertNull(schema.getType("Two"));
    }
    
    @Test
    public void explicitTypesTest() {
        GraphQLSchema schema = schemaFor(new FlatExplicitService());
        assertNotNull(schema.getType("One"));
        assertNotNull(schema.getType("Two"));
    }
    
    @Test
    public void discoveredTypesTest() {
        GraphQLSchema schema = schemaFor(new DeepService());
        assertNotNull(schema.getType("One"));
        assertNotNull(schema.getType("Two"));
    }
    
    @Test
    public void discoveredExplicitTypesTest() {
        GraphQLSchema schema = schemaFor(new DeepExplicitService());
        assertNotNull(schema.getType("One"));
        assertNotNull(schema.getType("Two"));
    }
    
    private GraphQLSchema schemaFor(Object service) {
        return new TestSchemaGenerator()
                .withOperationsFromSingleton(service)
                .generate();
    }
    
    @GraphQLInterface(name = "Flat")
    public interface Flat {}
    
    @GraphQLInterface(name = "Deep", implementationAutoDiscovery = true)
    public interface Deep {}

    public static class One implements Flat, Deep {
        public String getOne() {
            return "one";
        }
    }

    public static class Two implements Flat, Deep {
        public String getTwo() {
            return "two";
        }
    }

    public static class FlatService {
        @GraphQLQuery(name = "find")
        public Flat findFlat(@GraphQLArgument(name = "one") boolean one) {
            return one ? new One() : new Two();
        }
    }

    public static class FlatExplicitService extends FlatService {
        @GraphQLQuery(name = "one")
        public One findOne() {
            return new One();
        }
        
        @GraphQLQuery(name = "two")
        public Two findTwo() {
            return new Two();
        }
    }
    
    public static class DeepService {
        @GraphQLQuery(name = "find")
        public Deep findDeep(@GraphQLArgument(name = "one") boolean one) {
            return one ? new One() : new Two();
        }
    }

    public static class DeepExplicitService extends DeepService {
        @GraphQLQuery(name = "one")
        public One findOne() {
            return new One();
        }

        @GraphQLQuery(name = "two")
        public Two findTwo() {
            return new Two();
        }
    }
}
