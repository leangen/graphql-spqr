package io.leangen.graphql;

import io.leangen.graphql.domain.Person;
import io.leangen.graphql.domain.User;
import io.leangen.graphql.util.ClassUtils;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.lang.reflect.Proxy;

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
    public void testByteBuddyProxy() {
        Class<? extends Person> proxyObject = new ByteBuddy()
                .subclass(Person.class)
                .make()
                .load(Person.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertTrue(ClassUtils.isProxy(proxyObject));
    }
    
    @Test
    public void testJavassistProxy() {
        ProxyFactory f = new ProxyFactory();
        f.setSuperclass(User.class);
        f.setFilter(m -> !m.getName().equals("finalize"));
        Class<?> proxyClass = f.createClass();
        assertTrue(ClassUtils.isProxy(proxyClass));
    }
    
    @Test
    public void testNonProxy() {
        assertFalse(ClassUtils.isProxy(Person.class));
        assertFalse(ClassUtils.isProxy(Object.class));
        assertFalse(ClassUtils.isProxy(User.class));
    }
}
