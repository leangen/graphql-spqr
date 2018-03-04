package io.leangen.graphql.domain;

public class Human implements Character {
    private String name;
    private String nickName;

    public Human(String name, String nickName) {
        this.name = name;
        this.nickName = nickName;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getNickName() {
        return nickName;
    }
}
