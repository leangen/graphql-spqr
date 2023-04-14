package io.leangen.graphql.metadata.strategy.type;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphqlTypeComparatorEnvironment;
import graphql.schema.GraphqlTypeComparatorRegistry;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.types.GraphQLDirective;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.annotations.types.GraphQLUnion;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Utils;

import java.beans.Introspector;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.leangen.graphql.util.GraphQLUtils.name;

/**
 * @author Bojan Tomic (kaqqao)
 */
@SuppressWarnings("WeakerAccess")
public class DefaultTypeInfoGenerator implements TypeInfoGenerator {

    private boolean hierarchicalNames = false;
    private boolean staticAsTopLevel = false;
    private String genericTypeSeparator = "_";
    private String hierarchicalNameSeparator = "_";

    public static final GraphqlTypeComparatorRegistry DEFAULT_REGISTRY = new GraphqlTypeComparatorRegistry() {
        @Override
        public <T extends GraphQLSchemaElement> Comparator<? super T> getComparator(GraphqlTypeComparatorEnvironment env) {
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
            return generateParameterizedName((AnnotatedParameterizedType) type, messageBundle);
        }
        if (type instanceof AnnotatedArrayType) {
            return generateArrayName((AnnotatedArrayType) type, messageBundle);
        }
        Class<Object> rawType = ClassUtils.getRawType(type.getType());
        if (rawType.getEnclosingClass() != null && hierarchicalNames && isIncluded(rawType)) {
            //TODO Use AnnotatedType#getAnnotatedOwnerType instead of annotate(rawType.getEnclosingClass()) once available
            String enclosingName = generateTypeName(GenericTypeReflector.annotate(rawType.getEnclosingClass()), messageBundle);
            return enclosingName + hierarchicalNameSeparator + generateBaseName(type, messageBundle);
        }
        return generateBaseName(type, messageBundle);
    }

    private boolean isIncluded(Class<?> rawType) {
        return !Modifier.isStatic(rawType.getModifiers()) || !staticAsTopLevel;
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
            public <T extends GraphQLSchemaElement> Comparator<? super T> getComparator(GraphqlTypeComparatorEnvironment env) {
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

    protected String generateParameterizedName(AnnotatedParameterizedType type, MessageBundle messageBundle) {
        ParameterizedType parameterizedType = (ParameterizedType) type.getType();
        Class<?> rawType = ClassUtils.getRawType(type.getType());
        StringBuilder genericName = new StringBuilder();
        if (parameterizedType.getOwnerType() != null && hierarchicalNames && isIncluded(rawType)) {
            //TODO Use AnnotatedParameterizedType#getAnnotatedOwnerType instead of annotate(pType.getOwnerType()) once available
            String enclosingName = generateTypeName(GenericTypeReflector.annotate(parameterizedType.getOwnerType()), messageBundle);
            genericName.append(enclosingName).append(hierarchicalNameSeparator);
        }

        String baseName = generateBaseName(type, messageBundle);
        genericName.append(baseName);
        Arrays.stream(type.getAnnotatedActualTypeArguments())
                .map(t -> generateTypeName(t, messageBundle))
                .forEach(argName -> genericName.append(genericTypeSeparator).append(argName));
        return genericName.toString();
    }

    protected String generateArrayName(AnnotatedArrayType type, MessageBundle messageBundle) {
        return generateTypeName(type.getAnnotatedGenericComponentType(), messageBundle) + "Array";
    }

    @SuppressWarnings("unchecked")
    protected String generateBaseName(AnnotatedType type, MessageBundle messageBundle) {
        Optional<String>[] names = new Optional[]{
                Optional.ofNullable(type.getAnnotation(GraphQLUnion.class))
                        .map(GraphQLUnion::name),
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::name),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::name)
        };
        return messageBundle.interpolate(getFirstNonEmptyOrDefault(names, () -> ClassUtils.getRawType(type.getType()).getSimpleName()));
    }

    private String getFirstNonEmptyOrDefault(Optional<String>[] optionals, Supplier<String> defaultValue) {
        return Arrays.stream(optionals)
                .map(opt -> opt.filter(Utils::isNotEmpty))
                .reduce(Utils::or)
                .map(opt -> opt.orElse(defaultValue.get()))
                .get();
    }

    private String[] getFieldOrder(AnnotatedType type, MessageBundle messageBundle) {
        String[] fieldOrder = Utils.or(
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::fieldOrder)
                        .filter(Utils::isNotEmpty),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::fieldOrder)
                        .filter(Utils::isNotEmpty))
                .orElse(Utils.emptyArray());
        return Arrays.stream(fieldOrder).map(messageBundle::interpolate).toArray(String[]::new);
    }

    private String[] getInputFieldOrder(AnnotatedType type, MessageBundle messageBundle) {
        Optional<GraphQLType> annotation = Optional.ofNullable(type.getAnnotation(GraphQLType.class));
        String[] fieldOrder = Utils.or(
                annotation.map(GraphQLType::inputFieldOrder).filter(Utils::isNotEmpty),
                annotation.map(GraphQLType::fieldOrder).filter(Utils::isNotEmpty))
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
                .anyMatch(opt -> opt.filter(Utils::isNotEmpty).isPresent());
    }

    private <T extends GraphQLSchemaElement> Comparator<? super T> comparator(String[] givenOrder, GraphqlTypeComparatorEnvironment env) {
        if (givenOrder.length > 0) {
            return Comparator.comparingInt((T t) -> Utils.indexOf(givenOrder, name(t), Integer.MAX_VALUE))
                    .thenComparing(GraphQLUtils::name);
        }
        return DEFAULT_REGISTRY.getComparator(env);
    }

    public DefaultTypeInfoGenerator withHierarchicalNames() {
        return withHierarchicalNames(false);
    }

    public DefaultTypeInfoGenerator withHierarchicalNames(boolean staticAsTopLevel) {
        this.hierarchicalNames = true;
        this.staticAsTopLevel = staticAsTopLevel;
        return this;
    }

    public DefaultTypeInfoGenerator withNameSeparators(String genericTypeSeparator, String hierarchicalNameSeparator) {
        this.genericTypeSeparator = genericTypeSeparator;
        this.hierarchicalNameSeparator = hierarchicalNameSeparator;
        return this;
    }
}
