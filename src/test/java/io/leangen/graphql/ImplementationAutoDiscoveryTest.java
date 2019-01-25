package io.leangen.graphql;

import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.Argument;
import io.leangen.graphql.annotations.Query;
import io.leangen.graphql.annotations.types.Interface;
import io.leangen.graphql.annotations.types.Type;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests whether auto-discovered interface implementation types are correctly registered in the schema
 */
public class ImplementationAutoDiscoveryTest {

    @Test
    public void hiddenTypesTest() {
        GraphQLSchema schema = schemaFor(new ManualService());
        assertNull(schema.getType("One"));
        assertNull(schema.getType("Two"));
    }
    
    @Test
    public void explicitTypesTest() {
        GraphQLSchema schema = schemaFor(new ManualExplicitService());
        assertNotNull(schema.getType("One"));
        assertNotNull(schema.getType("Two"));
    }

    @Test
    public void discoveredTypesTest() {
        GraphQLSchema schema = schemaFor(new AutoService());
        assertNotNull(schema.getType("One"));
        assertNotNull(schema.getType("Two"));
    }
    
    @Test
    public void discoveredExplicitTypesTest() {
        GraphQLSchema schema = schemaFor(new AutoExplicitService());
        assertNotNull(schema.getType("One"));
        assertNotNull(schema.getType("Two"));
    }
    
    private GraphQLSchema schemaFor(Object service) {
        return new TestSchemaGenerator()
                .withOperationsFromSingleton(service)
                .generate();
    }

    @Type("Manual")
    @Interface
    interface Manual {}

    @Type("Auto")
    @Interface(implementationAutoDiscovery = true)
    interface Auto {}

    public static class One implements Manual, Auto {
        public String getOne() {
            return "one";
        }
    }

    public static class Two implements Manual, Auto {
        public String getTwo() {
            return "two";
        }
    }

    public static class ManualService {
        @Query(value = "find")
        public Manual findFlat(@Argument(value = "one") boolean one) {
            return one ? new One() : new Two();
        }
    }

    public static class ManualExplicitService extends ManualService {
        @Query(value = "one")
        public One findOne() {
            return new One();
        }
        
        @Query(value = "two")
        public Two findTwo() {
            return new Two();
        }
    }

    public static class AutoService {
        @Query(value = "find")
        public Auto findDeep(@Argument(value = "one") boolean one) {
            return one ? new One() : new Two();
        }
    }

    public static class AutoExplicitService extends AutoService {
        @Query(value = "one")
        public One findOne() {
            return new One();
        }

        @Query(value = "two")
        public Two findTwo() {
            return new Two();
        }
    }
}
