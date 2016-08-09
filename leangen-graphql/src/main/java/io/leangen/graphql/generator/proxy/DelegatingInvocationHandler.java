package io.leangen.graphql.generator.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 4/24/16.
 */
public class DelegatingInvocationHandler implements InvocationHandler {

	private Object delegate;

	public DelegatingInvocationHandler(Object delegate) {
		this.delegate = delegate;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return method.invoke(delegate, args);
	}
}