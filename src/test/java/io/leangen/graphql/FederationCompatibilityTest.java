package io.leangen.graphql;

import com.apollographql.federation.graphqljava.Federation;
import graphql.language.StringValue;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLDirective;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for https://github.com/leangen/graphql-spqr/issues/516
 *
 * Applied directive arguments must be stored as AST literals (e.g. StringValue),
 * not as raw Java objects. Apollo Federation reads @key(fields: ...) by casting
 * the argument value directly to StringValue, so valueProgrammatic caused a
 * ClassCastException: String cannot be cast to StringValue.
 */
public class FederationCompatibilityTest {

    @GraphQLDirective(name = "key")
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Key {
        String fields();
    }

    @Key(fields = "id")
    @GraphQLInterface(name = "FederatedEntity")
    public interface FederatedEntity {
        @GraphQLQuery
        String getId();
    }

    @Key(fields = "id")
    public static class FederatedEntityImpl implements FederatedEntity {
        @Override
        public String getId() { return "1"; }
    }

    public static class FederatedEntityService {
        @GraphQLQuery
        public FederatedEntityImpl entity() { return new FederatedEntityImpl(); }
    }

    @Test
    public void directiveArgumentsOnInterfacesAreStoredAsAstLiterals() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withBasePackages("io.leangen.graphql")
                .withOperationsFromSingleton(new FederatedEntityService())
                .generate();

        GraphQLInterfaceType interfaceType = schema.getTypeAs("FederatedEntity");
        assertNotNull(interfaceType);

        GraphQLAppliedDirective keyDirective = interfaceType.getAppliedDirective("key");
        assertNotNull("@key directive not found on interface", keyDirective);

        Object fieldsValue = keyDirective.getArgument("fields").getArgumentValue().getValue();
        assertTrue("Directive argument must be an AST StringValue, not a raw " + fieldsValue.getClass().getSimpleName(),
                fieldsValue instanceof StringValue);
        assertEquals("id", ((StringValue) fieldsValue).getValue());
    }

    @Test
    public void federationTransformSucceedsWithKeyDirectiveOnInterface() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withBasePackages("io.leangen.graphql")
                .withOperationsFromSingleton(new FederatedEntityService())
                .generate();

        GraphQLSchema federatedSchema = Federation.transform(schema)
                .fetchEntities(env -> null)
                .resolveEntityType(env -> env.getSchema().getObjectType("FederatedEntityImpl"))
                .build();

        // Federation adds _entities and _service fields to the query type
        GraphQLObjectType queryType = federatedSchema.getQueryType();
        assertNotNull(queryType.getField("_entities"));
        assertNotNull(queryType.getField("_service"));
    }
}
