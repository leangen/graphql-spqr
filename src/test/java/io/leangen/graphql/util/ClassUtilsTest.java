package io.leangen.graphql.util;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassUtilsTest {

    @Test
    public void testFindGetterForGet() {
        Optional<Method> fooGetter = ClassUtils.findGetter(ClassUtilsTest.class, "foo");
        assertTrue(fooGetter.isPresent());
        assertEquals(fooGetter.get().getName(), "getFoo");
    }

    @Test
    public void testFindGetterForIs() {
        Optional<Method> fooGetter = ClassUtils.findGetter(ClassUtilsTest.class, "bar");
        assertTrue(fooGetter.isPresent());
        assertEquals(fooGetter.get().getName(), "isBar");
    }

    @Test
    public void testFindGetterForMissingProperty() {
        Optional<Method> fooGetter = ClassUtils.findGetter(ClassUtilsTest.class, "fooBar");
        assertFalse(fooGetter.isPresent());
    }

    @SuppressWarnings("unused")
    public Object getFoo() {
        return null;
    }

    @SuppressWarnings("unused")
    public boolean isBar() {
        return false;
    }

}
