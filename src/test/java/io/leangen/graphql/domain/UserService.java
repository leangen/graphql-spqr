package io.leangen.graphql.domain;

import io.leangen.graphql.annotations.*;
import io.leangen.graphql.query.ConnectionRequest;

import java.util.*;

/**
 * Created by bojan.tomic on 3/5/16.
 */
public class UserService<T> {

    private Collection<Address> addresses = new ArrayList<>();

    public UserService() {
        Address address1 = new Address();
        address1.setTypes(Arrays.asList("residential", "home"));
        Street street11 = new Street("Vrolijkstraat", 300);
        Street street12 = new Street("Wibaustraat", 123);
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

    @GraphQLQuery(name = "users")
    public List<User<String>> getUsersById(ConnectionRequest context, @GraphQLArgument(name = "id") @RelayId Integer id) {
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

    @GraphQLQuery(name = "users")
    public List<User<String>> getUsersByEducation(@GraphQLArgument(name = "education") Education education) {
        return getUsersById(null, 1);
    }

//	@GraphQLQuery(name = "user")
//	public <G> G getUsersByMagic(@GraphQLArgument(name = "magic") int magic) {
//		return (G)getUserById(magic);
//	}

    @GraphQLQuery(name = "users")
    public List<User<String>> getUsersByAnyEducation(@GraphQLArgument(name = "educations") List<? super T> educations) {
        return getUsersById(null, 1);
    }

    @GraphQLQuery(name = "users")
    public List<User<String>> getUsersByRegDate(@GraphQLArgument(name = "regDate") Date date) {
        return getUsersById(null, 1);
    }

    @GraphQLMutation(name = "updateUsername")
    public User<String> updateUsername(@GraphQLResolverSource User<String> user, @GraphQLArgument(name = "username") String username) {
        user.name = username;
        return user;
    }

    //TODO figure out how to deal with void returns :: return source object instead?
//	@GraphQLMutation(name="user")
//	public void updateTitle(@GraphQLResolverSource User user, String title) {
//		user.title = title;
//	}

    @GraphQLQuery(name = "user")
    public User<String> getUserById(@GraphQLArgument(name = "id") @RelayId Integer id) {
        User<String> user = new User<>();
        user.id = 1;
        user.name = "One Dude";
        user.title = "The One";
        user.uuid = UUID.randomUUID();
        user.registrationDate = new Date();
        user.addresses = addresses;
        return user;
    }

    @GraphQLQuery(name = "users")
    public List<User<String>> getUserByUuid(@GraphQLArgument(name = "uuid") UUID uuid) {
        return getUsersById(null, 1);
    }

    //
//	@GraphQLQuery(name = "user")
//	public User getUserByName(@GraphQLArgument(name = "name") String name) {
//		User user = new User();
//		user.id = 1;
//		user.name = name;
//		user.addresses = addresses;
//		return user;
//	}
//
//	@GraphQLQuery(name = "names")
//	public List<String> getNames() {
//	 return Arrays.asList("macka", "bec");
//	}
//
//    @GraphQLQuery(name = "addresses", description = "Service :: 1st override")
//    public Page<Address> getByType(ConnectionRequest context, @GraphQLResolverSource User<String> owner, @GraphQLArgument(name = "type") String type) {
//        List<Address> addresses = owner.getAddresses().stream()
//                .filter(address -> address.getTypes().contains(type))
//                .collect(Collectors.toList());
//
//        return GraphQLUtils.createOffsetBasedPage(addresses, 20, 0);
//    }

    //
    @GraphQLQuery(name = "zmajs")
    public Collection<String> extraFieldAll(@GraphQLResolverSource User<String> source) {
        return Arrays.asList("zmaj", "azdaha");
    }
//
//	@GraphQLQuery(name = "zmajs", parentQueries = {"user","users"})
//	public Collection<String> extraFieldFiltered(@GraphQLResolverSource User source, @GraphQLArgument(name = "type") String type) {
//		return Arrays.asList("zmaj");
//	}
//
//	@GraphQLQuery(name = "addresses", parentQueries = "users")
//	public Collection<Address> getAll(ConnectionRequest context, @GraphQLResolverSource User<String> source) {
//		return source.addresses;
//	}
//
//	@GraphQLQuery(name = "streets", parentQueries = {"user.addresses"}) // or just "addresses" if there's no ambiguity
//	public Collection<Street> getByStreetName(ConnectionRequest context, @GraphQLResolverSource Address source, @GraphQLArgument(name = "name") String name) {
//		return source.getStreets().stream()
//				.filter(street -> street.getName().equals(name))
//				.collect(Collectors.toList());
//	}

//	@GraphQLQuery(name = "addresses", parentQueries = {"addresses.owner"})
//	public Collection<Address> getLandlordAddressByType(@GraphQLResolverSource User<String> source, @GraphQLArgument(name = "type") String type) {
//		return source.getAddresses().stream()
//				.filter(address -> address.getTypes().contains(type))
//				.collect(Collectors.toList());
//	}

//	@GraphQLQuery
//	public List<User> getNewestUsers() {
//		List<User> users = new ArrayList<>();
//		User u1 = new User();
//		u1.id = 666;
//		u1.name = "One";
//		u1.addresses = addresses;
//		User u2 = new User();
//		u2.id = 777;
//		u2.name = "Two";
//		users.add(u1);
//		users.add(u2);
//		return users;
//	}

//	@GraphQLQuery(name = "user", wrapper = User.class, attribute = "name")
//	public String getName() {
//		return "Jack!";
//	}

	@GraphQLQuery(name = "me")
	public Map<String, String> getCurrentUser() {
		Map<String, String> user = new HashMap<>();
		user.put("id", "1000");
		user.put("name", "Dyno");
		return user;
	}

    @GraphQLMutation(name = "upMe")
    public Map<String, String> getUpdateCurrentUser(@GraphQLArgument(name = "updates") Map<String, String> updates) {
        return updates;
    }
}
