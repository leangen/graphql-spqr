package io.leangen.graphql;

import javassist.util.proxy.ProxyFactory;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.FixedValue;

import org.junit.Test;

import java.lang.reflect.Proxy;

import io.leangen.graphql.domain.Person;
import io.leangen.graphql.domain.User;
import io.leangen.graphql.util.ClassUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProxyDetectionTest {
    
    @Test
    public void testJavaProxy() {
        Person proxyObject = (Person) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{Person.class},
                (proxy, method, args) -> null);
        assertTrue(ClassUtils.isProxy(proxyObject.getClass()));
    }
    
    @Test
    public void testCglibProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Person.class);
        enhancer.setCallback((FixedValue) () -> null);
        Person proxyObject = (Person) enhancer.create();
        assertTrue(ClassUtils.isProxy(proxyObject.getClass()));
    }
    
    @Test
    public void testJavassistProxy() throws IllegalAccessException, InstantiationException {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(User.class);
        f.setFilter(m -> !m.getName().equals("finalize"));
        Class proxyClass = f.createClass();
        assertTrue(ClassUtils.isProxy(proxyClass));
    }
    
    @Test
    public void testNonProxy() {
        assertFalse(ClassUtils.isProxy(Person.class));
        assertFalse(ClassUtils.isProxy(Object.class));
        assertFalse(ClassUtils.isProxy(User.class));
    }
}
