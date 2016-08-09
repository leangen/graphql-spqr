package io.leangen.graphql.generator.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.util.ClassUtils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

/**
 * Created by bojan.tomic on 5/6/16.
 */
public class ProxyFactory {

	private Map<Class, Class<RelayNodeProxy>> proxyTypeRegistry = new HashMap<>();

	public void registerType(Class<?> type) {
		try {
			proxyTypeRegistry.putIfAbsent(type, createProxyType(type));
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public Object proxy(Object target, GraphQLTypeHintProvider hintProvider) throws IllegalAccessException, InstantiationException {
		if (Iterable.class.isAssignableFrom(target.getClass())) {
			List<Object> result = new ArrayList<>();
			for (Object element : (Iterable) target) {
				result.add(proxy(element, hintProvider.getGraphQLTypeHint()));
			}
			return result;
		} else if (Page.class.isAssignableFrom(target.getClass())) {
			throw new IllegalArgumentException("Paged result can not be proxied");
		} else {
			return proxy(target, hintProvider.getGraphQLTypeHint());
		}
	}

	private Object proxy(Object target, String typeHint) throws IllegalAccessException, InstantiationException {
		RelayNodeProxy proxy;
		try {
			proxy = proxyTypeRegistry.get(target.getClass()).newInstance();
		} catch (InstantiationException e) {
			//TODO log a warning here
			proxy = ClassUtils.allocateInstance(proxyTypeRegistry.get(target.getClass()));
		}
		proxy.setDelegate(new DelegatingInvocationHandler(target));
		proxy.setGraphQLTypeHint(typeHint);
		return proxy;
	}

	@SuppressWarnings("unchecked")
	protected Class<RelayNodeProxy> createProxyType(Class clazz) throws IllegalAccessException, InstantiationException {
		return new ByteBuddy(ClassFileVersion.JAVA_V8)
				.subclass(clazz)
				.name(clazz.getName() + "$RelayNodeProxy")
				.implement(RelayNodeProxy.class)
				.intercept(FieldAccessor.ofBeanProperty())
				.method(isDeclaredBy(clazz))
				.intercept(InvocationHandlerAdapter.toInstanceField("delegate"))
				.defineField("graphQLTypeHint", String.class)
				.make()
				.load(clazz.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
				.getLoaded();
	}
}