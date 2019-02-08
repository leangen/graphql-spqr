package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.GraphQLId;
import org.eclipse.microprofile.graphql.Query;

public class Cat implements Pet {

    private final String passportNumber;
    private final String name;
    private final String sound;
    private final int clawLength;
    private Human owner;

    public Cat() {
        this("Cuddles");
    }

    public Cat(String name) {
        this("X123X", name,"meow", 3, new Human("Catherin", "Kat"));
    }

    public Cat(String passportNumber, String name, String sound, int clawLength, Human owner) {
        this.passportNumber = passportNumber;
        this.name = name;
        this.sound = sound;
        this.clawLength = clawLength;
        this.owner = owner;
    }

    @Query(value = "id")
    public @GraphQLId(relayId = true) String getPassportNumber() {
        return passportNumber;
    }

    @Override
    public String getSound() {
        return sound;
    }

    public int getClawLength() {
        return clawLength;
    }

    @Override
    public Human getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }
}
