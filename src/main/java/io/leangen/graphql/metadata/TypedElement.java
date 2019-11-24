package io.leangen.graphql.metadata;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.util.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TypedElement implements AnnotatedElement {

    private final AnnotatedType javaType;
    private final List<AnnotatedElement> elements;

    public TypedElement(AnnotatedType javaType, AnnotatedElement... elements) {
        this(javaType, Utils.asList(elements));
    }

    public TypedElement(AnnotatedType javaType, List<? extends AnnotatedElement> elements) {
        this.javaType = javaType;
        this.elements = elements.stream().distinct().collect(Collectors.toList());
    }

    public TypedElement(List<TypedElement> merge) {
        this.javaType = merge.stream().map(TypedElement::getJavaType).reduce(GenericTypeReflector::mergeAnnotations).get();
        this.elements = merge.stream().flatMap(e -> e.getElements().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return elements.stream().anyMatch(element -> element.isAnnotationPresent(annotation));
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return elements.stream()
                .filter(element -> element.isAnnotationPresent(annotationClass))
                .map(element -> element.getAnnotation(annotationClass))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Annotation[] getAnnotations() {
        return elements.stream().flatMap(element -> Arrays.stream(element.getAnnotations()))
                .distinct()
                .toArray(Annotation[]::new);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return elements.stream().flatMap(element -> Arrays.stream(element.getDeclaredAnnotations()))
                .distinct()
                .toArray(Annotation[]::new);
    }

    public boolean isAnnotationPresentAnywhere(Class<? extends Annotation> annotation) {
        return javaType.isAnnotationPresent(annotation) || isAnnotationPresent(annotation);
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    public List<AnnotatedElement> getElements() {
        return elements;
    }

    public AnnotatedElement getElement() {
        if (elements.size() == 1) {
            return elements.get(0);
        }
        throw new IllegalStateException("Multiple mappable elements found when a single was expected");
    }
}
