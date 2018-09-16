package io.leangen.graphql.metadata.strategy.query;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.types.GraphQLDirective;
import io.leangen.graphql.metadata.Directive;
import io.leangen.graphql.metadata.DirectiveArgument;
import io.leangen.graphql.metadata.strategy.value.AnnotationMappingUtils;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AnnotatedDirectiveBuilder implements DirectiveBuilder {

    @Override
    public List<Directive> buildSchemaDirectives(AnnotatedType schemaDescriptorType) {
        return buildDirectives(schemaDescriptorType);
    }

    @Override
    public List<Directive> buildObjectTypeDirectives(AnnotatedType type) {
        return buildDirectives(ClassUtils.getRawType(type.getType()));
    }

    @Override
    public List<Directive> buildScalarTypeDirectives(AnnotatedType type) {
        return buildDirectives(ClassUtils.getRawType(type.getType()));
    }

    @Override
    public List<Directive> buildFieldDefinitionDirectives(AnnotatedElement element) {
        return buildDirectives(element);
    }

    @Override
    public List<Directive> buildArgumentDefinitionDirectives(AnnotatedElement element) {
        return buildDirectives(element);
    }

    @Override
    public List<Directive> buildInterfaceTypeDirectives(AnnotatedType type) {
        return buildDirectives(ClassUtils.getRawType(type.getType()));
    }

    @Override
    public List<Directive> buildUnionTypeDirectives(AnnotatedType type) {
        return buildDirectives(ClassUtils.getRawType(type.getType()));
    }

    @Override
    public List<Directive> buildEnumTypeDirectives(AnnotatedType type) {
        return buildDirectives(ClassUtils.getRawType(type.getType()));
    }

    @Override
    public List<Directive> buildEnumValueDirectives(Enum<?> value) {
        return buildDirectives(ClassUtils.getEnumConstantField(value));
    }

    @Override
    public List<Directive> buildInputObjectTypeDirectives(AnnotatedType type) {
        return buildDirectives(ClassUtils.getRawType(type.getType()));
    }

    @Override
    public List<Directive> buildInputFieldDefinitionDirectives(AnnotatedElement element) {
        return buildDirectives(element);
    }

    private List<Directive> buildDirectives(AnnotatedElement element) {
        return Arrays.stream(element.getAnnotations())
                .filter(ann -> ann.annotationType().isAnnotationPresent(GraphQLDirective.class))
                .map(this::buildDirective)
                .collect(Collectors.toList());
    }

    private Directive buildDirective(Annotation annotation) {
        GraphQLDirective meta = annotation.annotationType().getAnnotation(GraphQLDirective.class);
        List<DirectiveArgument> arguments = ClassUtils.getAnnotationFields(annotation.annotationType()).stream()
                .map(method -> buildDirectiveArgument(annotation, method))
                .collect(Collectors.toList());
        return new Directive(Utils.coalesce(meta.name(), Introspector.decapitalize(annotation.annotationType().getSimpleName())), meta.description(), meta.locations(), arguments);
    }

    private DirectiveArgument buildDirectiveArgument(Annotation annotation, Method method) {
        try {
            return new DirectiveArgument(AnnotationMappingUtils.inputFieldName(method), AnnotationMappingUtils.inputFieldDescription(method),
                    GenericTypeReflector.annotate(method.getReturnType()), method.invoke(annotation), method, annotation);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
