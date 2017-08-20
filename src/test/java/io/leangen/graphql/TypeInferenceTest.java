package io.leangen.graphql;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.generator.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypeInferenceTest {

    @Test
    public void testClasses() throws AnnotationFormatException {
        Annotation[] nonNull = new Annotation[] {TypeFactory.annotation(Nonnull.class, Collections.emptyMap())};
        Annotation[] graphNonNull = new Annotation[] {TypeFactory.annotation(GraphQLNonNull.class, Collections.emptyMap())};
        Annotation[] mergedAnnotations = new Annotation[] {nonNull[0], graphNonNull[0]};
        AnnotatedType c1 = GenericTypeReflector.annotate(C1.class, nonNull);
        AnnotatedType c2 = GenericTypeReflector.annotate(C2.class, graphNonNull);
        AnnotatedType expected = GenericTypeReflector.annotate(P.class, mergedAnnotations);
        AnnotatedType inferred = ClassUtils.getCommonSuperType(Arrays.asList(c1, c2));
        assertTrue(GenericTypeReflector.equals(expected, inferred));
    }

    @Test
    public void testClassesWithInterface() throws AnnotationFormatException {
        Annotation[] annotations = new Annotation[] {TypeFactory.annotation(Nonnull.class, Collections.emptyMap())};
        AnnotatedType t1 = GenericTypeReflector.annotate(T1.class, annotations);
        AnnotatedType t2 = GenericTypeReflector.annotate(T2.class);
        AnnotatedType expected = GenericTypeReflector.annotate(I.class, annotations);
        AnnotatedType inferred = ClassUtils.getCommonSuperType(Arrays.asList(t1, t2));
        assertTrue(GenericTypeReflector.equals(expected, inferred));
    }

    @Test
    public void testSameClasses() throws AnnotationFormatException {
        Annotation[] nonNull = new Annotation[] {TypeFactory.annotation(Nonnull.class, Collections.emptyMap())};
        Annotation[] graphQLNonNull = new Annotation[] {TypeFactory.annotation(GraphQLNonNull.class, Collections.emptyMap())};
        Annotation[] mergedAnnotations = new Annotation[] {nonNull[0], graphQLNonNull[0]};
        AnnotatedType p1 = GenericTypeReflector.annotate(P.class, nonNull);
        AnnotatedType p2 = GenericTypeReflector.annotate(P.class, graphQLNonNull);
        AnnotatedType expected = GenericTypeReflector.annotate(P.class, mergedAnnotations);
        AnnotatedType inferred = ClassUtils.getCommonSuperType(Arrays.asList(p1, p2));
        assertTrue(GenericTypeReflector.equals(expected, inferred));
    }
    
    @Test(expected = TypeMappingException.class)
    public void testComparableClasses() throws AnnotationFormatException {
        AnnotatedType t1 = GenericTypeReflector.annotate(Long.class);
        AnnotatedType t2 = GenericTypeReflector.annotate(String.class);
        ClassUtils.getCommonSuperType(Arrays.asList(t1, t2)).getType();
    }
    
    @Test(expected = TypeMappingException.class)
    public void testIncompatibleClasses() throws AnnotationFormatException {
        AnnotatedType t1 = GenericTypeReflector.annotate(Locale.class);
        AnnotatedType t2 = GenericTypeReflector.annotate(UUID.class);
        ClassUtils.getCommonSuperType(Arrays.asList(t1, t2)).getType();
    }

    @Test
    public void testAllowedIncompatibleClasses() throws AnnotationFormatException {
        AnnotatedType t1 = GenericTypeReflector.annotate(String.class);
        AnnotatedType t2 = GenericTypeReflector.annotate(Integer.class);
        AnnotatedType fallback = GenericTypeReflector.annotate(Object.class);
        AnnotatedType inferred = ClassUtils.getCommonSuperType(Arrays.asList(t1, t2), fallback);
        assertEquals(fallback, inferred);
    }

    @Test
    public void testExplicitRootTypes() throws AnnotationFormatException {
        AnnotatedType t1 = new TypeToken<Comparable<Long>>(){}.getAnnotatedType();
        AnnotatedType t2 = new TypeToken<Comparable<Double>>(){}.getAnnotatedType();
        Type expected = new TypeToken<Comparable<Number>>(){}.getType();
        Type inferred = ClassUtils.getCommonSuperType(Arrays.asList(t1, t2)).getType();
        assertEquals(expected, inferred);
    }
    
    @Test
    public void testInterfaces() throws AnnotationFormatException {
        Annotation[] annotations = new Annotation[] {TypeFactory.annotation(Nonnull.class, Collections.emptyMap())};
        AnnotatedType i1 = GenericTypeReflector.annotate(I1.class, annotations);
        AnnotatedType i2 = GenericTypeReflector.annotate(I2.class);
        AnnotatedType expected = GenericTypeReflector.annotate(I.class, annotations);
        AnnotatedType inferred = ClassUtils.getCommonSuperType(Arrays.asList(i1, i2));
        assertTrue(GenericTypeReflector.equals(expected, inferred));
    }
    
    @Test(expected = TypeMappingException.class)
    public void testIncompatibleInterfaces() throws AnnotationFormatException {
        AnnotatedType t1 = GenericTypeReflector.annotate(I.class);
        AnnotatedType t2 = GenericTypeReflector.annotate(II.class);
        ClassUtils.getCommonSuperType(Arrays.asList(t1, t2)).getType();
    }

    @Test(expected = TypeMappingException.class)
    public void testAnnotations() throws AnnotationFormatException {
        AnnotatedType t1 = GenericTypeReflector.annotate(Nonnull.class);
        AnnotatedType t2 = GenericTypeReflector.annotate(GraphQLNonNull.class);
        ClassUtils.getCommonSuperType(Arrays.asList(t1, t2)).getType();
    }
    
    @Test(expected = TypeMappingException.class)
    public void testRawTypes() throws AnnotationFormatException {
        AnnotatedType t1 = GenericTypeReflector.annotate(ArrayList.class);
        AnnotatedType t2 = GenericTypeReflector.annotate(LinkedList.class);
        ClassUtils.getCommonSuperType(Arrays.asList(t1, t2));
    }
    
    @Test(expected = TypeMappingException.class)
    public void testIncompleteArrayTypes() throws AnnotationFormatException {
        AnnotatedType t1 = GenericTypeReflector.annotate(ArrayList[].class);
        AnnotatedType t2 = GenericTypeReflector.annotate(LinkedList[].class);
        ClassUtils.getCommonSuperType(Arrays.asList(t1, t2));
    }
    
    @Test(expected = TypeMappingException.class)
    public void testIncompleteListTypes() throws AnnotationFormatException {
        AnnotatedType t1 = new TypeToken<ArrayList<ArrayList<ArrayList<String>[]>>[]>(){}.getAnnotatedType();
        AnnotatedType t2 = new TypeToken<LinkedList<LinkedList<LinkedList[]>>[]>(){}.getAnnotatedType();
        ClassUtils.getCommonSuperType(Arrays.asList(t1, t2));
    }
    
    @Test(expected = TypeMappingException.class)
    public void testMixedRawTypes() throws AnnotationFormatException {
        AnnotatedType t1 = new TypeToken<ArrayList<String>>(){}.getAnnotatedType();
        AnnotatedType t2 = GenericTypeReflector.annotate(LinkedList.class);
        ClassUtils.getCommonSuperType(Arrays.asList(t1, t2));
    }

    @Test
    public void testUnboundedWildcardTypes() throws AnnotationFormatException {
        AnnotatedType t1 = new TypeToken<ArrayList<?>>(){}.getAnnotatedType();
        AnnotatedType t2 = new TypeToken<LinkedList<?>>(){}.getAnnotatedType();
        AnnotatedType expected = new TypeToken<AbstractList<?>>(){}.getAnnotatedType();
        assertTrue(GenericTypeReflector.equals(expected, ClassUtils.getCommonSuperType(Arrays.asList(t1, t2))));
    }
    
    @Test
    public void testMixedWildcardTypes() throws AnnotationFormatException {
        AnnotatedType t1 = new TypeToken<ArrayList<? extends Long>>(){}.getAnnotatedType();
        AnnotatedType t2 = new TypeToken<LinkedList<Double>>(){}.getAnnotatedType();
        AnnotatedType expected = new TypeToken<AbstractList<Number>>(){}.getAnnotatedType();
        assertTrue(GenericTypeReflector.equals(expected, ClassUtils.getCommonSuperType(Arrays.asList(t1, t2))));
    }
    
    @Test
    public void testLists() throws AnnotationFormatException {
        Annotation[] annotations = new Annotation[] {TypeFactory.annotation(GraphQLNonNull.class, Collections.emptyMap())};
        AnnotatedType nonNullLongType = GenericTypeReflector.annotate(Long.class, annotations);
        AnnotatedType doubleType = GenericTypeReflector.annotate(Double.class);
        AnnotatedType nonNullNumberType = TypeFactory.parameterizedAnnotatedClass(Number.class, annotations);
        AnnotatedType expected = TypeFactory.parameterizedAnnotatedClass(AbstractList.class, annotations, nonNullNumberType);
        AnnotatedType list1 = TypeFactory.parameterizedAnnotatedClass(ArrayList.class, annotations, nonNullLongType);
        AnnotatedType list2 = TypeFactory.parameterizedAnnotatedClass(LinkedList.class, new Annotation[0], doubleType);
        AnnotatedType inferred = ClassUtils.getCommonSuperType(Arrays.asList(list1, list2));
        assertTrue(GenericTypeReflector.equals(expected, inferred));
    }

    @Test
    public void testMaps() throws AnnotationFormatException {
        Annotation[] annotations = new Annotation[] {TypeFactory.annotation(GraphQLNonNull.class, Collections.emptyMap())};
        AnnotatedType nonNullLongType = GenericTypeReflector.annotate(Long.class, annotations);
        AnnotatedType doubleType = GenericTypeReflector.annotate(Double.class);
        AnnotatedType nonNullNumberType = TypeFactory.parameterizedAnnotatedClass(Number.class, annotations);
        AnnotatedType i1Type = GenericTypeReflector.annotate(I1.class);
        AnnotatedType i2Type = GenericTypeReflector.annotate(I2.class, annotations);
        AnnotatedType iType = GenericTypeReflector.annotate(I.class, annotations);
        AnnotatedType expected = TypeFactory.parameterizedAnnotatedClass(Map.class, annotations, nonNullNumberType, iType);
        AnnotatedType map1 = TypeFactory.parameterizedAnnotatedClass(Map.class, annotations, nonNullLongType, i1Type);
        AnnotatedType map2 = TypeFactory.parameterizedAnnotatedClass(Map.class, new Annotation[0], doubleType, i2Type);
        AnnotatedType inferred = ClassUtils.getCommonSuperType(Arrays.asList(map1, map2));
        assertTrue(GenericTypeReflector.equals(expected, inferred));
    }

    @Test
    public void testArrays() throws AnnotationFormatException {
        Annotation[] graphQlNonNull = new Annotation[] {TypeFactory.annotation(GraphQLNonNull.class, Collections.emptyMap())};
        Annotation[] nonNull = new Annotation[] {TypeFactory.annotation(Nonnull.class, Collections.emptyMap())};
        Annotation[] empty = new Annotation[0];
        AnnotatedType a1 = TypeFactory.arrayOf(GenericTypeReflector.annotate(Long.class, graphQlNonNull), nonNull);
        AnnotatedType a2 = TypeFactory.arrayOf(GenericTypeReflector.annotate(Double.class, empty), empty);
        AnnotatedType nonNullNumberType = TypeFactory.parameterizedAnnotatedClass(Number.class, graphQlNonNull);
        AnnotatedType expected = TypeFactory.arrayOf(nonNullNumberType, nonNull);
        AnnotatedType inferred = ClassUtils.getCommonSuperType(Arrays.asList(a1, a2));
        assertTrue(GenericTypeReflector.equals(expected, inferred));
    }
    
    private interface I {}
    private interface II {}
    private interface I1 extends I {}
    private interface I2 extends I {}
    private static class T1 implements I {}
    private static class T2 implements I {}
    private static class P {}
    private static class C1 extends P{}
    private static class C2 extends P{}
}
