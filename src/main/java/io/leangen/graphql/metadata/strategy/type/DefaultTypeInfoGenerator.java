package io.leangen.graphql.metadata.strategy.type;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphqlTypeComparatorEnvironment;
import graphql.schema.GraphqlTypeComparatorRegistry;
import io.leangen.graphql.annotations.types.GraphQLDirective;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.annotations.types.GraphQLUnion;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.beans.Introspector;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class DefaultTypeInfoGenerator implements TypeInfoGenerator {

    public static final GraphqlTypeComparatorRegistry DEFAULT_REGISTRY = new GraphqlTypeComparatorRegistry() {
        @Override
        public <T extends graphql.schema.GraphQLType> Comparator<? super T> getComparator(GraphqlTypeComparatorEnvironment env) {
            //Leave the arguments in the declared order
            if (env.getElementType().equals(GraphQLArgument.class)) {
                return GraphqlTypeComparatorRegistry.AS_IS_REGISTRY.getComparator(env);
            }
            //Sort everything else by name
            return GraphqlTypeComparatorRegistry.BY_NAME_REGISTRY.getComparator(env);
        }
    };

    @Override
    public String generateTypeName(AnnotatedType type, MessageBundle messageBundle) {
        if (type instanceof AnnotatedParameterizedType) {
            String baseName = generateSimpleName(type, messageBundle);
            StringBuilder genericName = new StringBuilder(baseName);
            Arrays.stream(((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments())
                    .map(t -> generateTypeName(t, messageBundle))
                    .forEach(argName -> genericName.append("_").append(argName));
            return genericName.toString();
        }
        return generateSimpleName(type, messageBundle);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generateTypeDescription(AnnotatedType type, MessageBundle messageBundle) {
        Optional<String>[] descriptions = new Optional[]{
                Optional.ofNullable(type.getAnnotation(GraphQLUnion.class))
                        .map(GraphQLUnion::description),
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::description),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::description)
        };
        return messageBundle.interpolate(getFirstNonEmptyOrDefault(descriptions, () -> ""));
    }

    @Override
    public String generateDirectiveTypeName(AnnotatedType type, MessageBundle messageBundle) {
        return messageBundle.interpolate(
                Optional.ofNullable(type.getAnnotation(GraphQLDirective.class))
                        .map(GraphQLDirective::name)
                        .filter(Utils::isNotEmpty)
                        .orElse(Introspector.decapitalize(ClassUtils.getRawType(type.getType()).getSimpleName())));
    }

    @Override
    public String generateDirectiveTypeDescription(AnnotatedType type, MessageBundle messageBundle) {
        return messageBundle.interpolate(
                Optional.ofNullable(type.getAnnotation(GraphQLDirective.class))
                        .map(GraphQLDirective::description)
                        .orElse(""));
    }

    @Override
    public GraphqlTypeComparatorRegistry generateComparatorRegistry(AnnotatedType type, MessageBundle messageBundle) {
        if (!isOrdered(type)) {
            return DEFAULT_REGISTRY;
        }
        return new GraphqlTypeComparatorRegistry() {
            @Override
            public <T extends graphql.schema.GraphQLType> Comparator<? super T> getComparator(GraphqlTypeComparatorEnvironment env) {
                if (env.getElementType().equals(GraphQLFieldDefinition.class)) {
                    return comparator(getFieldOrder(type, messageBundle), env);
                }
                if (env.getElementType().equals(GraphQLInputObjectField.class)
                        || env.getElementType().equals(GraphQLEnumValueDefinition.class)) {
                    return comparator(getInputFieldOrder(type, messageBundle), env);
                }
                return DEFAULT_REGISTRY.getComparator(env);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private String generateSimpleName(AnnotatedType type, MessageBundle messageBundle) {
        Optional<String>[] names = new Optional[]{
                Optional.ofNullable(type.getAnnotation(GraphQLUnion.class))
                        .map(GraphQLUnion::name),
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::name),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::name)
        };
        return messageBundle.interpolate(getFirstNonEmptyOrDefault(names, () -> getSimpleName(ClassUtils.getRawType(type.getType()))));
    }

    private String getFirstNonEmptyOrDefault(Optional<String>[] optionals, Supplier<String> defaultValue) {
        return Arrays.stream(optionals)
                .map(opt -> opt.filter(Utils::isNotEmpty))
                .reduce(Utils::or)
                .map(opt -> opt.orElse(defaultValue.get()))
                .get();
    }

    private String getSimpleName(Class<?> clazz) {
        if (clazz.isArray()) {
            return getSimpleName(clazz.getComponentType()) + "Array";
        }
        return clazz.getSimpleName();
    }

    private String[] getFieldOrder(AnnotatedType type, MessageBundle messageBundle) {
        String[] fieldOrder = Utils.or(
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::fieldOrder)
                        .filter(Utils::isArrayNotEmpty),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::fieldOrder)
                        .filter(Utils::isArrayNotEmpty))
                .orElse(Utils.emptyArray());
        return Arrays.stream(fieldOrder).map(messageBundle::interpolate).toArray(String[]::new);
    }

    private String[] getInputFieldOrder(AnnotatedType type, MessageBundle messageBundle) {
        Optional<GraphQLType> annotation = Optional.ofNullable(type.getAnnotation(GraphQLType.class));
        String[] fieldOrder = Utils.or(
                annotation.map(GraphQLType::inputFieldOrder).filter(Utils::isArrayNotEmpty),
                annotation.map(GraphQLType::fieldOrder).filter(Utils::isArrayNotEmpty))
                .orElse(Utils.emptyArray());
        return Arrays.stream(fieldOrder).map(messageBundle::interpolate).toArray(String[]::new);
    }

    private boolean isOrdered(AnnotatedType type) {
        return Stream.of(
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::fieldOrder),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::fieldOrder),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::inputFieldOrder))
                .anyMatch(opt -> opt.filter(Utils::isArrayNotEmpty).isPresent());
    }

    private <T extends graphql.schema.GraphQLType> Comparator<? super T> comparator(String[] givenOrder, GraphqlTypeComparatorEnvironment env) {
        if (givenOrder.length > 0) {
            return Comparator.comparingInt((T t) -> Utils.indexOf(givenOrder, t.getName(), Integer.MAX_VALUE))
                    .thenComparing(graphql.schema.GraphQLType::getName);
        }
        return DEFAULT_REGISTRY.getComparator(env);
    }
}
