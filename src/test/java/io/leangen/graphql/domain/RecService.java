package io.leangen.graphql.domain;

import java.util.ArrayList;
import java.util.List;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;

/**
 * Created by bojan.tomic on 6/24/16.
 */
public class RecService {

    @GraphQLQuery(name = "user")
    public SimpleUser getByName(@GraphQLArgument(name = "name") @GraphQLId(relayId = true) String name) {
        return new SimpleUser(name);
    }

    @GraphQLQuery(name = "user")
    public SimpleUser getByFriends(@GraphQLArgument(name = "friends") @GraphQLNonNull List<@GraphQLNonNull SimpleUser> friends) {
        return new SimpleUser("Friendly McFriend", friends.get(0));
    }

    @GraphQLQuery(name = "getEducation")
    public Education getName(@GraphQLArgument(name = "user") SimpleUser user) {
        return new Education("aaa", 12, 12);
    }

    @GraphQLQuery(name = "educations")
    public @GraphQLNonNull List<@GraphQLNonNull Education> getEdus(@GraphQLArgument(name = "user") @GraphQLNonNull SimpleUser user) {
        List<Education> edus = new ArrayList<>();
        edus.add(new Education("aaa", 12, 12));
        return edus;
    }

    @GraphQLQuery(name = "extra")
    public String extra(@GraphQLContext @GraphQLArgument(name = "education") Education source) {
        return "zmajs";
    }

//	@GraphQLQuery(name = "phreak", parentQueries = "user.education")
//	public String freak(@GraphQLContext @GraphQLArgument(name = "education") Education source) {
//		return "phreak";
//	}
}
