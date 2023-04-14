package io.leangen.graphql.generator.mapping;

import io.leangen.graphql.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IgnoredAnnotationsTypeComparator implements Comparator<AnnotatedType> {

    private Predicate<Class<? extends Annotation>> annotationFilter;

    public IgnoredAnnotationsTypeComparator() {
        this(annotation -> true);
    }

    @SuppressWarnings("WeakerAccess")
    public IgnoredAnnotationsTypeComparator(Predicate<Class<? extends Annotation>> annotationFilter) {
        this.annotationFilter = annotationFilter;
    }

    @SafeVarargs
    public final IgnoredAnnotationsTypeComparator exclude(Class<? extends Annotation>... annotationTypes) {
        this.annotationFilter = this.annotationFilter.and(blacklist(annotationTypes));
        return this;
    }

    public IgnoredAnnotationsTypeComparator exclude(String... packages) {
        this.annotationFilter = this.annotationFilter.and(blacklist(packages));
        return this;
    }

    @SafeVarargs
    public final IgnoredAnnotationsTypeComparator include(Class<? extends Annotation>... annotationTypes) {
        this.annotationFilter = this.annotationFilter.and(blacklist(annotationTypes).negate());
        return this;
    }

    public IgnoredAnnotationsTypeComparator include(String... packages) {
        this.annotationFilter = this.annotationFilter.and(blacklist(packages).negate());
        return this;
    }

    private static Predicate<Class<? extends Annotation>> blacklist(Class<? extends Annotation>[] annotationTypes) {
        Set<Class<? extends Annotation>> ignored = new HashSet<>();
        Collections.addAll(ignored, annotationTypes);
        return annotation -> !ignored.contains(annotation);
    }

    private static Predicate<Class<? extends Annotation>> blacklist(String[] packages) {
        return annotation -> Arrays.stream(packages).noneMatch(ignored -> ClassUtils.isSubPackage(annotation.getPackage(), ignored));
    }

    @Override
    public int compare(AnnotatedType o1, AnnotatedType o2) {
        Class<?> r1 = ClassUtils.getRawType(o1.getType());
        Class<?> r2 = ClassUtils.getRawType(o2.getType());
        return (r1.equals(r2) && annotationsMatch(o1, o2)) ? 0 : -1;
    }

    private boolean annotationsMatch(AnnotatedType o1, AnnotatedType o2) {
        Set<? extends Class<? extends Annotation>> a1 = Arrays.stream(o1.getAnnotations())
                .map(Annotation::annotationType)
                .collect(Collectors.toSet());
        Set<? extends Class<? extends Annotation>> a2 = Arrays.stream(o2.getAnnotations())
                .map(Annotation::annotationType)
                .collect(Collectors.toSet());

        return a1.stream().noneMatch(ann -> !a2.contains(ann) && annotationFilter.test(ann))
                && a2.stream().noneMatch(ann -> !a1.contains(ann) && annotationFilter.test(ann));
    }
}
