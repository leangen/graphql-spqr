package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.leangen.graphql.annotations.GraphQLQuery;

/**
 * Created by bojan.tomic on 7/20/16.
 */
public abstract class Executable {

	private static final Set<List<String>> ROOT_QUERY_ONLY = Collections.singleton(Collections.singletonList(GraphQLQuery.ROOT_QUERY_ALIAS));
	
	protected AnnotatedElement delegate;
	
	abstract public Object execute(Object target, Object[] args) throws InvocationTargetException, IllegalAccessException;
	abstract public AnnotatedType getReturnType();
	abstract public int getParameterCount();
	abstract public AnnotatedType[] getAnnotatedParameterTypes();
	abstract public Parameter[] getParameters();


	/**
	 * Resolves what attribute this partial resolver returns. Partial resolvers are methods that do not return a whole
	 * object that is to be sent to the client, but only its one attribute (e.g. only a username instead of the entire User object).
	 *
	 * @return The name of the attribute this partial resolver returns
	 */
	abstract public String getWrappedAttribute();

	public Set<List<String>> getParentTrails() {
		if (!delegate.isAnnotationPresent(GraphQLQuery.class) ||
				delegate.getAnnotation(GraphQLQuery.class).parentQueries().length == 0 ||
				(delegate.getAnnotation(GraphQLQuery.class).parentQueries().length == 1 &&
						delegate.getAnnotation(GraphQLQuery.class).parentQueries()[0].equals(GraphQLQuery.ROOT_QUERY_ALIAS))) {
			return ROOT_QUERY_ONLY;
		}
		return Arrays.stream(delegate.getAnnotation(GraphQLQuery.class).parentQueries())
				.map(parentExpression -> Arrays.asList(parentExpression.split("\\.")))
				.collect(Collectors.toSet());
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Executable && ((Executable) other).delegate.equals(this.delegate);
	}
}