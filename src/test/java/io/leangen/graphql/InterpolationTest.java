package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.metadata.messages.SimpleMessageBundle;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class InterpolationTest {

    @Test
    public void testInterpolation() {
        Map<String, String> m = new HashMap<>();
        m.put("mutation.dish.name", "Cool");
        m.put("mutation.dish.desc", "Make a dish as cool as a 🥒");
        m.put("mutation.dish.deprecation", "No longer needed");
        m.put("mutation.dish.arg.name", "theDish");
        m.put("mutation.dish.arg.desc", "The dish to be chilled");
        m.put("type.dish.name", "TastyDish");
        m.put("type.dish.desc", "An uncommonly tasty dish");
        m.put("type.dish.arg.name", "temp");
        m.put("type.dish.arg.desc", "The dish's temperature");
        m.put("type.dish.arg.default", "\"Kewl\"");
        m.put("type.temp.cool.name", "Kewl");
        m.put("type.temp.cool.desc", "🥒");
        m.put("type.temp.cool.deprecation", "Too cold");
        m.put("type.temp.hot.name", "Hawt");
        m.put("type.temp.hot.desc", "🌶");
        m.put("type.temp.hot.deprecation", "Too hot");
        GraphQLSchema schema = new TestSchemaGenerator()
                .withTypeAdapters(new MapToListTypeAdapter<>())
                .withOperationsFromSingleton(new Quick())
                .withStringInterpolation(new SimpleMessageBundle(m))
                .generate();
    }

    private static class Quick {

        @GraphQLMutation(name = "makeIt${mutation.dish.name}", description = "DESCRIPTION: ${mutation.dish.desc}", deprecationReason = "REASON: ${mutation.dish.deprecation}")
        public Dish test(@GraphQLArgument(name = "${mutation.dish.arg.name}", description = "${mutation.dish.arg.desc}") Dish dish) {
            return dish;
        }
    }

    @GraphQLType(name = "${type.dish.name}", description = "Description: ${type.dish.desc}")
    private static class Dish {

        private final Temperature temperature;

        private Dish(Temperature temperature) {
            this.temperature = temperature;
        }

        @JsonCreator
        public static Dish make(@GraphQLInputField(name = "${type.dish.arg.name}", description = "${type.dish.arg.desc}", defaultValue = "${type.dish.arg.default}") Temperature temperature) {
            return new Dish(temperature);
        }

        public Temperature getTemperature() {
            return temperature;
        }

        enum Temperature {
            @GraphQLEnumValue(name = "${type.temp.cool.name}", description = "${type.temp.cool.desc}", deprecationReason = "${type.temp.cool.deprecation}") COOL,
            @GraphQLEnumValue(name = "${type.temp.hot.name}", description = "${type.temp.hot.desc}", deprecationReason = "${type.temp.hot.deprecation}") HOT
        }
    }
}
