package io.leangen.graphql.util;

import java.util.Deque;
import java.util.List;

/**
 * Created by bojan.tomic on 5/20/16.
 */
public class ListUtils {

	@SuppressWarnings("unchecked")
	public static <T> T getLast(List<T> list) {
		if (list instanceof Deque) {
			return ((Deque<T>)list).getLast();
		}
		return list.get(list.size() - 1);
	}
}
