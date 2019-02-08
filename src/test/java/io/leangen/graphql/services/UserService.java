package io.leangen.graphql.services;

import io.leangen.graphql.annotations.GraphQLComplexity;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.domain.Address;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.domain.User;
import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by bojan.tomic on 3/5/16.
 */
public class UserService<T> {

    private Collection<Address> addresses = new ArrayList<>();

    public UserService() {
        Address address1 = new Address();
        address1.setTypes(Arrays.asList("residential", "home"));
        Street street11 = new Street("Fakestreet", 300);
        Street street12 = new Street("Realstreet", 123);
        address1.getStreets().add(street11);
        address1.getStreets().add(street12);
        Address address2 = new Address();
        address2.setTypes(Collections.singletonList("office"));
        Street street21 = new Street("Oneway street", 100);
        Street street22 = new Street("Twowaystreet", 200);
        address2.getStreets().add(street21);
        address2.getStreets().add(street22);
        this.addresses.add(address1);
        this.addresses.add(address2);
    }

    @Query(value = "users")
    public List<User<String>> getUsersById(@Argument(value = "id") @GraphQLId Integer id) {
        User<String> user = new User<>();
        user.id = id;
        user.name = "Tatko";
        user.uuid = UUID.randomUUID();
        user.registrationDate = new Date();
        user.addresses = addresses;
        User<String> user2 = new User<>();
        user2.id = id + 1;
        user2.name = "Tzar";
        user2.uuid = UUID.randomUUID();
        user2.registrationDate = new Date();
        user2.addresses = addresses;
        return Arrays.asList(user, user2);
    }

    @Query(value = "users")
    public List<User<String>> getUsersByEducation(@Argument(value = "education") Education education) {
        return getUsersById(1);
    }

//	@GraphQLQuery(name = "user")
//	public <G> G getUsersByMagic(@GraphQLArgument(name = "magic") int magic) {
//		return (G)getUserById(magic);
//	}

    @Query(value = "users")
    public List<User<String>> getUsersByAnyEducation(@Argument(value = "educations") List<? super T> educations) {
        return getUsersById(1);
    }

    @Query(value = "usersArr")
    @SuppressWarnings("unchecked")
    public User<String>[] getUsersByAnyEducationArray(@Argument(value = "educations") T[] educations) {
        List<User<String>> users = getUsersById(1);
        return users.toArray(new User[0]);
    }

    @Query(value = "users")
    @GraphQLComplexity("2 * childScore")
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public List<User<String>> getUsersByRegDate(@Argument(value = "regDate") Optional<Date> date) {
        return getUsersById(1);
    }

    @Query(value = "usersByDate")
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public List<User<String>> getUsersByDate(@Argument(value = "regDate") Optional<Date> date) {
        return getUsersById(1);
    }

    @Mutation(value = "updateUsername")
    public User<String> updateUsername(@Source User<String> user, @Argument(value = "username") String username) {
        user.name = username;
        return user;
    }

    //TODO figure out how to deal with void returns :: return source object instead?
//	@GraphQLMutation(name="user")
//	public void updateTitle(@GraphQLContext User user, String title) {
//		user.title = title;
//	}

    @Query(value = "user")
    public User<String> getUserById(@GraphQLId(relayId = true) Integer wonkyName) {
        User<String> user = new User<>();
        user.id = 1;
        user.name = "One Dude";
        user.title = "The One";
        user.uuid = UUID.randomUUID();
        user.registrationDate = new Date();
        user.addresses = addresses;
        return user;
    }

    @Query(value = "users")
    public List<User<String>> getUserByUuid(@Argument(value = "uuid") UUID uuid) {
        return getUsersById(1);
    }

    @Query(value = "zmajs")
    public Collection<String> extraFieldAll(@Source User<String> source) {
        return Arrays.asList("zmaj", "azdaha");
    }

    @Query(value = "me")
    public Map<String, String> getCurrentUser() {
        Map<String, String> user = new HashMap<>();
        user.put("id", "1000");
        user.put("name", "Dyno");
        return user;
    }

    @Mutation(value = "upMe")
    public Map<String, String> getUpdateCurrentUser(@Argument(value = "updates") Map<String, String> updates) {
        return updates;
    }
}
