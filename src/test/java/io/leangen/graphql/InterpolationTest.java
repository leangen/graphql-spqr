package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.metadata.messages.SimpleMessageBundle;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.Defaults;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InterpolationTest {

    @Test
    public void testInterpolation() {
        Map<String, String> translations = new HashMap<>();
        translations.put("mutation.dish.name", "Cool");
        translations.put("mutation.dish.desc", "Make a dish as cool as a 🥒");
        translations.put("mutation.dish.deprecation", "No longer needed");
        translations.put("mutation.dish.arg.name", "theDish");
        translations.put("mutation.dish.arg.desc", "The dish to be chilled");
        translations.put("mutation.dish.arg.default", "{\"temp\":\"Hawt\"}");
        translations.put("type.dish.name", "TastyDish");
        translations.put("type.dish.desc", "An uncommonly tasty dish");
        translations.put("type.dish.fields.temp.name", "temp");
        translations.put("type.dish.fields.temp.desc", "The dish's temperature");
        translations.put("type.dish.fields.temp.default", "\"Kewl\"");
        translations.put("type.temp.name", "Hotness");
        translations.put("type.temp.desc", "How hot it is");
        translations.put("type.temp.cool.name", "Kewl");
        translations.put("type.temp.cool.desc", "🥒");
        translations.put("type.temp.cool.deprecation", "Too cold");
        translations.put("type.temp.hot.name", "Hawt");
        translations.put("type.temp.hot.desc", "🌶");
        translations.put("type.temp.hot.deprecation", "Too hot");

        ExecutableSchema executableSchema = new TestSchemaGenerator()
                .withTypeAdapters(new MapToListTypeAdapter())
                .withOperationsFromSingleton(new Quick())
                .withStringInterpolation(new SimpleMessageBundle(translations))
                .generateExecutable();

        GraphQLSchema schema = executableSchema.getSchema();
        ValueMapper valueMapper = Defaults.valueMapperFactory(new DefaultTypeInfoGenerator())
                .getValueMapper(Collections.emptyMap(), executableSchema.globalEnvironment);

        GraphQLFieldDefinition mutation = schema.getMutationType().getFieldDefinition("makeIt" + translations.get("mutation.dish.name"));
        assertNotNull(mutation);
        assertEquals("DESCRIPTION: " + translations.get("mutation.dish.desc"), mutation.getDescription());
        assertEquals("REASON: " + translations.get("mutation.dish.deprecation"), mutation.getDeprecationReason());

        graphql.schema.GraphQLArgument mutationArgument = mutation.getArgument(translations.get("mutation.dish.arg.name"));
        assertNotNull(mutationArgument);
        assertEquals(translations.get("mutation.dish.arg.desc"), mutationArgument.getDescription());
        Dish value = valueMapper.fromInput(mutationArgument.getArgumentDefaultValue().getValue(), TypeFactory.annotatedClass(Dish.class, new Annotation[0]));
        assertEquals(Dish.Temperature.HOT, value.getTemperature());

        GraphQLInputObjectType dish = (GraphQLInputObjectType) mutationArgument.getType();
        assertEquals(translations.get("type.dish.name") + "Input", dish.getName());
        assertEquals("Description: " + translations.get("type.dish.desc"), dish.getDescription());

        GraphQLInputObjectField temperatureField = dish.getFieldDefinition(translations.get("type.dish.fields.temp.name"));
        assertNotNull(temperatureField);
        assertEquals(translations.get("type.dish.fields.temp.desc"), temperatureField.getDescription());
        Object value1 = valueMapper.fromInput(temperatureField.getInputFieldDefaultValue().getValue(), TypeFactory.annotatedClass(Dish.Temperature.class, new Annotation[0]));
        assertEquals(Dish.Temperature.COOL, value1);

        GraphQLEnumType temperature = (GraphQLEnumType) temperatureField.getType();
        assertEquals(translations.get("type.temp.name"), temperature.getName());
        assertEquals(translations.get("type.temp.desc"), temperature.getDescription());

        GraphQLEnumValueDefinition cool = temperature.getValue(translations.get("type.temp.cool.name"));
        assertNotNull(cool);
        assertEquals(translations.get("type.temp.cool.name"), cool.getName());
        assertEquals(translations.get("type.temp.cool.desc"), cool.getDescription());
        assertEquals(translations.get("type.temp.cool.deprecation"), cool.getDeprecationReason());

        GraphQLEnumValueDefinition hot = temperature.getValue(translations.get("type.temp.hot.name"));
        assertNotNull(hot);
        assertEquals(translations.get("type.temp.hot.name"), hot.getName());
        assertEquals(translations.get("type.temp.hot.desc"), hot.getDescription());
        assertEquals(translations.get("type.temp.hot.deprecation"), hot.getDeprecationReason());
    }

    private static class Quick {
        @GraphQLQuery
        public String makeSchemaValidationPass;

        @GraphQLMutation(name = "makeIt${mutation.dish.name}", description = "DESCRIPTION: ${mutation.dish.desc}", deprecationReason = "REASON: ${mutation.dish.deprecation}")
        public Dish test(@GraphQLArgument(name = "${mutation.dish.arg.name}", description = "${mutation.dish.arg.desc}", defaultValue = "${mutation.dish.arg.default}") Dish dish) {
            return dish;
        }
    }

    @GraphQLType(name = "${type.dish.name}", description = "Description: ${type.dish.desc}")
    private static class Dish {

        public String makeSchemaValidationPass;

        private final Temperature temperature;

        private Dish(Temperature temperature) {
            this.temperature = temperature;
        }

        @JsonCreator
        public static Dish make(@GraphQLInputField(name = "${type.dish.fields.temp.name}", description = "${type.dish.fields.temp.desc}", defaultValue = "${type.dish.fields.temp.default}") Temperature temperature) {
            return new Dish(temperature);
        }

        Temperature getTemperature() {
            return temperature;
        }

        @GraphQLType(name = "${type.temp.name}", description = "${type.temp.desc}")
        enum Temperature {
            @GraphQLEnumValue(name = "${type.temp.cool.name}", description = "${type.temp.cool.desc}", deprecationReason = "${type.temp.cool.deprecation}") COOL,
            @GraphQLEnumValue(name = "${type.temp.hot.name}", description = "${type.temp.hot.desc}", deprecationReason = "${type.temp.hot.deprecation}") HOT
        }
    }
}
