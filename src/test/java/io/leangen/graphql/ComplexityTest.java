package io.leangen.graphql;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLComplexity;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.execution.complexity.ComplexityLimitExceededException;
import io.leangen.graphql.services.UserService;

import static io.leangen.graphql.support.Matchers.complexityScore;

public class ComplexityTest {

    private static final String fragmentQuery = "{" +
            "  newUsers: users(regDate: 1465667452785) {" +
            "    name" +
            "    home: addresses {" +
            "      streets {" +
            "        name" +
            "      }" +
            "    }" +
            "    ...userInfo" +
            "    uuid" +
            "  }" +
            "}" +
            "" +
            "fragment userInfo on User_String {" +
            "  name" +
            "  title" +
            "  regDate" +
            "  addresses {" +
            "    types" +
            "    streets {" +
            "      name" +
            "      number" +
            "    }" +
            "  }" +
            "}";

    private static final String branchingQuery = "{" +
            "  pet(cat: true) {" +
            "    sound" +
            "    owner {" +
            "       name" +
            "    }" +
            "    ... on Cat {" +
            "      clawLength" +
            "    }" +
            "    ... on Dog {" +
            "       boneCount" +
            "       owner {" +
            "           nickName @skip(if: true)" +
            "       }" +
            "    }" +
            "  }" +
            "}";
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void fragmentComplexityTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new UserService<Education>(), new TypeToken<UserService<Education>>(){}.getAnnotatedType())
                .generate();

        GraphQL exe = GraphQLRuntime.newGraphQL(schema)
                .maximumQueryComplexity(11)
                .build();

        thrown.expect(ComplexityLimitExceededException.class);
        thrown.expect(complexityScore(32));
        exe.execute(fragmentQuery);
    }
    
    @Test
    public void branchingComplexityTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new PetService())
                .generate();

        GraphQL exe = GraphQLRuntime.newGraphQL(schema)
                .maximumQueryComplexity(5)
                .build();

        thrown.expect(ComplexityLimitExceededException.class);
        thrown.expect(complexityScore(6));
        exe.execute(branchingQuery);
    }
    
    @GraphQLInterface(name = "Pet", implementationAutoDiscovery = true)
    public interface Pet {
        String getSound();
        Human getOwner();
    }
    
    public static class Cat implements Pet {
        @Override
        public String getSound() {
            return "meow";
        }
        
        public int getClawLength() {
            return 3;
        }

        @Override
        public Human getOwner() {
            return new Human("Catherin", "Kat");
        }
    }
    
    public static class Dog implements Pet {
        @Override
        public String getSound() {
            return "woof";
        }
        
        @GraphQLComplexity("2")
        public int getBoneCount() {
            return 5;
        }

        @Override
        public Human getOwner() {
            return new Human("John", "Dawg");
        }
    }
    
    public static class Human {
        private String name;
        private String nickName;

        public Human(String name, String nickName) {
            this.name = name;
            this.nickName = nickName;
        }

        public String getName() {
            return name;
        }

        public String getNickName() {
            return nickName;
        }
    }
    
    public static class PetService {
        @GraphQLQuery(name = "pet")
        public Pet findPet(@GraphQLArgument(name = "cat") boolean cat) {
            return cat ? new Cat() : new Dog();
        }
    }
}
