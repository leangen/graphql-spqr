package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.annotations.GraphQLQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static io.leangen.graphql.support.GraphQLTypeAssertions.assertEnum;
import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(Parameterized.class)
public class PolymorphicJacksonTest {

    @Parameterized.Parameter
    public boolean abstractInputResolverEnabled;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Object[] data() {
        return new Object[] { Boolean.TRUE, Boolean.FALSE };
    }

    @Test
    public void testExplicitSubtypeInfoOnParentWithDiscriminator() {
        checkTypeDiscriminator(new Service<>(), Vehicle.class, "Vehicle", "vehicleType", "CarX", "TruckX");
    }

    @Test
    public void testExplicitSubtypeInfoOnParentNoDiscriminator() {
        checkTypeDiscriminator(new Service<>(), Event.class, "Event", "_type_", "ConcertX", "ExhibitionX");
    }

    @Test
    public void testExplicitSubtypeInfoOnChildWithDiscriminator() {
        checkTypeDiscriminator(new Service<>(), Pet.class, "Pet", "species", "CatX", "DogX");
    }

    @Test
    public void testExplicitSubtypeInfoOnChildNoDiscriminator() {
        checkTypeDiscriminator(new Service<>(), Nut.class, "Nut", "_type_", "WalnutX", "ChestnutX");
    }

    @Test
    public void testSingleExplicitSubtypeWithDiscriminator() {
        checkTypeDiscriminator(new Service<>(), Game.class, "Game", "kind", "CardGameX");
    }

    @Test
    public void testSingleExplicitSubtypeNoDiscriminator() {
        checkTypeDiscriminator(new Service<>(), Abstract.class, "Abstract", "_type_", "ConcreteX");
    }

    private <T extends Named> void checkTypeDiscriminator(Service<T> service, Class<T> serviceType, String typeName, String discriminatorField, String... subTypes) {
        GraphQLSchemaGenerator generator = new TestSchemaGenerator()
                .withOperationsFromSingleton(service, TypeFactory.parameterizedClass(Service.class, serviceType));
        if (abstractInputResolverEnabled) {
            generator.withAbstractInputTypeResolution();
        }
        GraphQLSchema schema = generator
                .generate();

        GraphQLType vehicleTypeDisambiguator = schema.getType(typeName + "TypeDisambiguator");
        assertNotNull(vehicleTypeDisambiguator);
        assertEnum(vehicleTypeDisambiguator, subTypes);
        GraphQLInputObjectField vehicleType = ((GraphQLInputObjectType) schema.getType(typeName + "Input"))
                .getFieldDefinition(discriminatorField);
        assertNotNull(vehicleType);
        assertSame(vehicleTypeDisambiguator, vehicleType.getType());

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{" +
                "test (in: {" +
                "       name: \"yay\"" +
                "       " +  discriminatorField + ": " + subTypes[0] + "}) {" +
                "   name}}");
        assertNoErrors(result);
        assertValueAtPathEquals("yay" + subTypes[0], result, "test.name");
    }

    public static class Service<T extends Named> {
        @GraphQLQuery
        public T test(T in) {
            in.name += in.getClass().getSimpleName() + "X";
            return in;
        }
    }

    //TODO Consider supporting field-level disambiguation
    /*public static class OnField {

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "vehicleType")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = Car.class, name = "CarX"),
                @JsonSubTypes.Type(value = Truck.class, name = "TruckX")
        })
        public Vehicle vehicle;
    }*/

    public abstract static class Named {
        public String name;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "vehicleType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Car.class, name = "CarX"),
            @JsonSubTypes.Type(value = Truck.class, name = "TruckX")
    })
    public abstract static class Vehicle extends Named {
        public String regNumber;
    }

    public static class Car extends Vehicle {
        public String carField = "Car";
    }

    public static class Truck extends Vehicle {
        public String truckField = "Truck";
    }

    @JsonSubTypes({
            @JsonSubTypes.Type(value = Concert.class, name = "ConcertX"),
            @JsonSubTypes.Type(value = Exhibition.class, name = "ExhibitionX")
    })
    public abstract static class Event extends Named {
        public String venue;
    }

    public static class Concert extends Event {
        public String bandName = "b4nD";
    }

    public static class Exhibition extends Event {
        public String artist = "Le Painter";
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "species")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Cat.class),
            @JsonSubTypes.Type(value = Dog.class)
    })
    public abstract static class Pet extends Named {
        public String sound;
    }

    @JsonTypeName("CatX")
    public static class Cat extends Pet {
        public int clawLength = 99;
    }

    @JsonTypeName("DogX")
    public static class Dog extends Pet {
        public int barkVolume = 99;
    }

    @JsonSubTypes({
            @JsonSubTypes.Type(value = Walnut.class),
            @JsonSubTypes.Type(value = Chestnut.class)
    })
    public abstract static class Nut extends Named {
        public String weight;
    }

    @JsonTypeName("WalnutX")
    public static class Walnut extends Nut {
        public int smell = 99;
    }

    @JsonTypeName("ChestnutX")
    public static class Chestnut extends Nut {
        public int color = 99;
    }

    @JsonSubTypes(@JsonSubTypes.Type(value = Concrete.class, name = "ConcreteX"))
    public abstract static class Abstract extends Named {
        public String abs;
    }

    public static class Concrete extends Abstract {
        public int thickness = 99;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes(@JsonSubTypes.Type(value = CardGame.class, name = "CardGameX"))
    public abstract static class Game extends Named {
        public String duration;
    }

    public static class CardGame extends Game {
        public int deckType = 99;
    }
}
