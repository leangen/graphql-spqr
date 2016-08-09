package io.leangen.graphql;

import java.util.List;

import org.junit.Test;

import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.User;
import io.leangen.graphql.domain.UserService;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.util.GraphQLUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by bojan.tomic on 3/5/16.
 */
public class SchemaTest {

	private static final String nodeQuery = "{node(id: \"dXNlcjox\") {... on user {" +
			"      name" +
			"    }" +
			"... on Node {" +
			"      id" +
			"    }" +
			"}}";

	private static final String simpleQuery = "{users(regDate: 1465667452785) {" +
												"id, name, addresses {" +
													"types" +
												"}}}";

	private static final String connectionQuery = "{user(id: \"dXNlcjox\") {" +
												"id, name, addresses(after:\"azx\" first:6 type:\"office\") {" +
														"pageInfo {" +
															"hasNextPage" +
														"}, " +
														"edges {" +
															"cursor, node {" +
																"types, owner {" +
																	"addresses(type:\"creep\") {" +
																		"types" +
												"}}}}}}}";

	private static final String complexInputQuery = "{users (education: {" +
														"schoolName: \"tshc\"," +
														"startYear: 1999," +
														"endYear: 2003}) {" +
													"name" +
												"}}";

	private static final String complexGenericInputQuery = "{users (educations: [" +
																	"{schoolName: \"tshc\"," +
																	"startYear: 1999," +
																	"endYear: 2003}," +

																	"{schoolName: \"other\"," +
																	"startYear: 1999," +
																	"endYear: 2003}]) {" +
																"name" +
															"}}";

	private static final String mutation = "mutation M {" +
			"  updateUsername(username: \"OMG NEW USERNAME!!11\") {" +
			"    id" +
			"    name" +
			"    addresses {" +
			"      types" +
			"    }" +
			"  }" +
			"}";

	@Test
	public void testIdBasedPageCreation() {
		List<User<String>> users = new UserService<Education>().getUsersById(null, 1);
		Page<User<String>> userPage = GraphQLUtils.createIdBasedPage(users, 1, 99);
		assertTrue(userPage.getPageInfo().isHasNextPage());
		assertFalse(userPage.getPageInfo().isHasPreviousPage());
	}

	@Test
	public void testOffsetBasedPageCreation() {
		List<User<String>> users = new UserService<String>().getUsersById(null, 1);
		Page<User<String>> userPage = GraphQLUtils.createOffsetBasedPage(users, 5, 0);
		assertTrue(userPage.getPageInfo().isHasNextPage());
		assertFalse(userPage.getPageInfo().isHasPreviousPage());
	}
}