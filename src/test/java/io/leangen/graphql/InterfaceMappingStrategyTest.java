package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.generator.mapping.strategy.AnnotatedInterfaceStrategy;
import io.leangen.graphql.util.ClassUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InterfaceMappingStrategyTest {

    @Test
    public void testInterfaceSelection() {
        Collection<Class<?>> interfaces = new AnnotatedInterfaceStrategy()
                .getInterfaces(GenericTypeReflector.annotate(Child.class)).stream()
                .map(inter -> ClassUtils.getRawType(inter.getType()))
                .collect(Collectors.toList());

        assertEquals(6, interfaces.size());
        assertTrue(interfaces.containsAll(Arrays.asList(Zero.class, One.class, Two.class, Three.class, Four.class, Parent.class)));
    }

    @Test
    public void testClassMapping() {
        Collection<Class<?>> interfaces = new AnnotatedInterfaceStrategy()
                .withClassMapping(false)
                .getInterfaces(GenericTypeReflector.annotate(Child.class)).stream()
                .map(inter -> ClassUtils.getRawType(inter.getType()))
                .collect(Collectors.toList());

        assertEquals(5, interfaces.size());
        assertTrue(interfaces.containsAll(Arrays.asList(Zero.class, One.class, Two.class, Three.class, Four.class)));
    }

    @Test
    public void testInterfaceFiltering() {
        Collection<Class<?>> interfaces = new AnnotatedInterfaceStrategy()
                .withFilters(inter -> inter != One.class)
                .getInterfaces(GenericTypeReflector.annotate(Three.class)).stream()
                .map(inter -> ClassUtils.getRawType(inter.getType()))
                .collect(Collectors.toList());

        assertEquals(3, interfaces.size());
        assertTrue(interfaces.containsAll(Arrays.asList(Zero.class, Two.class, Three.class)));
    }

    @GraphQLInterface(name = "Zero")
    private interface Zero {}

    @GraphQLInterface(name = "One")
    private interface One {}

    @GraphQLInterface(name = "Two")
    private interface Two extends One {}

    @GraphQLInterface(name = "Three")
    private interface Three extends Zero, Two {}

    @GraphQLInterface(name = "Four")
    private interface Four {}

    @GraphQLInterface(name = "Parent")
    private static abstract class Parent implements Four {}

    private static class Child extends Parent implements Zero, Three {}
}
