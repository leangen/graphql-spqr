package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.Directive;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

public interface DirectiveBuilder {

    default List<Directive> buildSchemaDirectives(AnnotatedType schemaDescriptorType) {
        return Collections.emptyList();
    }

    default List<Directive> buildObjectTypeDirectives(AnnotatedType type) {
        return Collections.emptyList();
    }

    default List<Directive> buildScalarTypeDirectives(AnnotatedType type) {
        return Collections.emptyList();
    }

    default List<Directive> buildFieldDefinitionDirectives(AnnotatedElement element) {
        return Collections.emptyList();
    }

    default List<Directive> buildArgumentDefinitionDirectives(AnnotatedElement element) {
        return Collections.emptyList();
    }

    default List<Directive> buildInterfaceTypeDirectives(AnnotatedType type) {
        return Collections.emptyList();
    }

    default List<Directive> buildUnionTypeDirectives(AnnotatedType type) {
        return Collections.emptyList();
    }

    default List<Directive> buildEnumTypeDirectives(AnnotatedType type) {
        return Collections.emptyList();
    }

    default List<Directive> buildEnumValueDirectives(Enum<?> value) {
        return Collections.emptyList();
    }

    default List<Directive> buildInputObjectTypeDirectives(AnnotatedType type) {
        return Collections.emptyList();
    }

    default List<Directive> buildInputFieldDefinitionDirectives(AnnotatedElement element) {
        return Collections.emptyList();
    }
}
