package io.leangen.graphql;

import io.leangen.graphql.execution.ResolverInterceptorFactory;
import io.leangen.graphql.generator.mapping.*;
import io.leangen.graphql.generator.mapping.common.IdAdapter;
import io.leangen.graphql.generator.mapping.common.ScalarMapper;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface GeneratorConfigurer<T extends GeneratorConfigurer<T>> {

    /**
     * Registers custom {@link TypeMapper}s to be used for mapping Java type to GraphQL types.
     * <p><b>Ordering of mappers is strictly important as the first {@link TypeMapper} that supports the given Java type
     * will be used for mapping it.</b></p>
     * <p>See {@link TypeMapper#supports(java.lang.reflect.AnnotatedElement, AnnotatedType)}</p>
     *
     * @param typeMappers Custom type mappers to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    default T withTypeMappers(TypeMapper... typeMappers) {
        return withTypeMappers((conf, current) -> current.insertAfterOrAppend(IdAdapter.class, typeMappers));
    }

    T withTypeMappers(ExtensionProvider<GeneratorConfiguration, TypeMapper> provider);

    /**
     * Registers custom {@link InputConverter}s to be used for converting values provided by the GraphQL client
     * into those expected by the corresponding Java method. Only needed in some specific cases when usual deserialization
     * isn't enough, for example, when a client-provided {@link java.util.List} should be repackaged into a {@link java.util.Map},
     * which is normally done because GraphQL type system has no direct support for maps.
     * <p><b>Ordering of converters is strictly important as the first {@link InputConverter} that supports the given Java type
     * will be used for converting it.</b></p>
     * <p>See {@link InputConverter#supports(AnnotatedType)}</p>
     *
     * @param inputConverters Custom input converters to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    default T withInputConverters(InputConverter<?,?>... inputConverters) {
        return withInputConverters((config, current) -> current.insert(0, inputConverters));
    }

    T withInputConverters(ExtensionProvider<GeneratorConfiguration, InputConverter> provider);

    /**
     * Registers custom {@link OutputConverter}s to be used for converting values returned by the exposed Java method
     * into those expected by the GraphQL client. Only needed in some specific cases when usual serialization isn't enough,
     * for example, when an instance of {@link java.util.Map} should be repackaged into a {@link java.util.List}, which
     * is normally done because GraphQL type system has no direct support for maps.
     * <p><b>Ordering of converters is strictly important as the first {@link OutputConverter} that supports the given Java type
     * will be used for converting it.</b></p>
     * <p>See {@link OutputConverter#supports(java.lang.reflect.AnnotatedElement, AnnotatedType)}</p>
     *
     * @param outputConverters Custom output converters to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    default T withOutputConverters(OutputConverter<?,?>... outputConverters) {
        return withOutputConverters((config, current) -> current.insertAfterOrAppend(IdAdapter.class, outputConverters));
    }

    T withOutputConverters(ExtensionProvider<GeneratorConfiguration, OutputConverter> provider);

    default T withArgumentInjectors(ArgumentInjector... argumentInjectors) {
        return withArgumentInjectors((config, current) -> current.insert(0, argumentInjectors));
    }

    T withArgumentInjectors(ExtensionProvider<GeneratorConfiguration, ArgumentInjector> provider);

    /**
     * Globally registers {@link ResolverBuilder}s to be used for sources that don't have explicitly assigned builders.
     *
     * @param resolverBuilders builders to be globally registered
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    default T withResolverBuilders(ResolverBuilder... resolverBuilders) {
        return withResolverBuilders((config, defaults) -> Arrays.asList(resolverBuilders));
    }

    T withResolverBuilders(ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider);

    /**
     * Globally registers {@link ResolverBuilder}s to be used for sources that don't have explicitly assigned builders.
     *
     * @param resolverBuilders builders to be globally registered
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    default T withNestedResolverBuilders(ResolverBuilder... resolverBuilders) {
        return withNestedResolverBuilders((config, defaults) -> Arrays.asList(resolverBuilders));
    }

    T withNestedResolverBuilders(ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider);

    default T withInputFieldBuilders(InputFieldBuilder... inputFieldBuilders) {
        return withInputFieldBuilders((env, defaults) -> defaults.prepend(inputFieldBuilders));
    }

    /**
     * Type adapters (instances of {@link AbstractTypeAdapter}) are both type mappers and bidirectional converters,
     * implementing {@link TypeMapper}, {@link InputConverter} and {@link OutputConverter}.
     * They're used in the same way as mappers/converters individually, and exist solely because it can sometimes
     * be convenient to group the logic for mapping and converting to/from the same Java type in one place.
     * For example, because GraphQL type system has no notion of maps, {@link java.util.Map}s require special logic
     * both when mapping them to a GraphQL type and when converting them before and after invoking a Java method.
     * For this reason, all code dealing with translating {@link java.util.Map}s is kept in one place in
     * {@link io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter}.
     * <p><b>Ordering of mappers/converters is strictly important as the first one supporting the given Java type
     * will be used to map/convert it.</b></p>
     * <p>See {@link #withTypeMappers(ExtensionProvider)}</p>
     * <p>See {@link #withInputConverters(ExtensionProvider)}</p>
     * <p>See {@link #withOutputConverters(ExtensionProvider)}</p>
     *
     * @param typeAdapters Custom type adapters to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    default T withTypeAdapters(AbstractTypeAdapter<?,?>... typeAdapters) {
        withInputConverters(typeAdapters);
        withOutputConverters(typeAdapters);
        return withTypeMappers((conf, defaults) -> defaults.insertAfter(ScalarMapper.class, typeAdapters));
    }

    T withInputFieldBuilders(ExtensionProvider<ExtendedGeneratorConfiguration, InputFieldBuilder> provider);

    T withResolverInterceptorFactories(ExtensionProvider<GeneratorConfiguration, ResolverInterceptorFactory> provider);

    default T withSchemaTransformers(SchemaTransformer... transformers) {
        return withSchemaTransformers((conf, current) -> current.append(transformers));
    }

    T withSchemaTransformers(ExtensionProvider<GeneratorConfiguration, SchemaTransformer> provider);

    T withTypeInfoGenerator(TypeInfoGenerator typeInfoGenerator);

    default T withTypeComparator(Type... synonymGroup) {
        return withTypeComparators((config, comparators) -> comparators.append(new SynonymBaseTypeComparator(synonymGroup)));
    }

    T withTypeComparators(ExtensionProvider<GeneratorConfiguration, Comparator<AnnotatedType>> provider);
}
