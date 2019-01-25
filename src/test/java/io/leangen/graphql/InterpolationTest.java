package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.Argument;
import io.leangen.graphql.annotations.DefaultValue;
import io.leangen.graphql.annotations.Deprecated;
import io.leangen.graphql.annotations.EnumValue;
import io.leangen.graphql.annotations.InputField;
import io.leangen.graphql.annotations.Mutation;
import io.leangen.graphql.annotations.types.Type;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.metadata.messages.SimpleMessageBundle;
import org.junit.Test;

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

        GraphQLSchema schema = new TestSchemaGenerator()
                .withTypeAdapters(new MapToListTypeAdapter<>())
                .withOperationsFromSingleton(new Quick())
                .withStringInterpolation(new SimpleMessageBundle(translations))
                .generate();

        GraphQLFieldDefinition mutation = schema.getMutationType().getFieldDefinition("makeIt" + translations.get("mutation.dish.name"));
        assertNotNull(mutation);
        assertEquals("DESCRIPTION: " + translations.get("mutation.dish.desc"), mutation.getDescription());
        assertEquals("REASON: " + translations.get("mutation.dish.deprecation"), mutation.getDeprecationReason());

        graphql.schema.GraphQLArgument mutationArgument = mutation.getArgument(translations.get("mutation.dish.arg.name"));
        assertNotNull(mutationArgument);
        assertEquals(translations.get("mutation.dish.arg.desc"), mutationArgument.getDescription());
        assertEquals(Dish.Temperature.HOT, ((Dish) mutationArgument.getDefaultValue()).getTemperature());

        GraphQLInputObjectType dish = (GraphQLInputObjectType) mutationArgument.getType();
        assertEquals(translations.get("type.dish.name") + "Input", dish.getName());
        assertEquals("Description: " + translations.get("type.dish.desc"), dish.getDescription());

        GraphQLInputObjectField temperatureField = dish.getFieldDefinition(translations.get("type.dish.fields.temp.name"));
        assertNotNull(temperatureField);
        assertEquals(translations.get("type.dish.fields.temp.desc"), temperatureField.getDescription());
        assertEquals(Dish.Temperature.COOL, temperatureField.getDefaultValue());

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

        @Mutation(value = "makeIt${mutation.dish.name}", description = "DESCRIPTION: ${mutation.dish.desc}")
        @Deprecated("REASON: ${mutation.dish.deprecation}")
        public Dish test(@Argument(value = "${mutation.dish.arg.name}", description = "${mutation.dish.arg.desc}") @DefaultValue("${mutation.dish.arg.default}") Dish dish) {
            return dish;
        }
    }

    @Type(value = "${type.dish.name}", description = "Description: ${type.dish.desc}")
    private static class Dish {

        private final Temperature temperature;

        private Dish(Temperature temperature) {
            this.temperature = temperature;
        }

        @JsonCreator
        public static Dish make(@InputField(name = "${type.dish.fields.temp.name}", description = "${type.dish.fields.temp.desc}") @DefaultValue("${type.dish.fields.temp.default}") Temperature temperature) {
            return new Dish(temperature);
        }

        Temperature getTemperature() {
            return temperature;
        }

        @Type(value = "${type.temp.name}", description = "${type.temp.desc}")
        enum Temperature {
            @EnumValue(value = "${type.temp.cool.name}", description = "${type.temp.cool.desc}") @Deprecated("${type.temp.cool.deprecation}") COOL,
            @EnumValue(value = "${type.temp.hot.name}", description = "${type.temp.hot.desc}") @Deprecated("${type.temp.hot.deprecation}") HOT
        }
    }
}
