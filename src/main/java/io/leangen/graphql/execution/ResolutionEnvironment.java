package io.leangen.graphql.execution;

import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.generator.mapping.ConverterRegistry;
import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.Urls;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bojan Tomic (kaqqao)
 */
@SuppressWarnings("WeakerAccess")
public class ResolutionEnvironment {

    public final Object context;
    public final Object rootContext;
    public final Resolver resolver;
    public final ValueMapper valueMapper;
    public final GlobalEnvironment globalEnvironment;
    public final GraphQLOutputType fieldType;
    public final GraphQLNamedType parentType;
    public final GraphQLSchema graphQLSchema;
    public final DataFetchingEnvironment dataFetchingEnvironment;
    public final Map<String, Object> arguments;

    private final ConverterRegistry converters;
    private final DerivedTypeRegistry derivedTypes;

    public ResolutionEnvironment(Resolver resolver, DataFetchingEnvironment env, ValueMapper valueMapper, GlobalEnvironment globalEnvironment,
                                 ConverterRegistry converters, DerivedTypeRegistry derivedTypes) {

        this.context = env.getSource();
        this.rootContext = env.getContext();
        this.resolver = resolver;
        this.valueMapper = valueMapper;
        this.globalEnvironment = globalEnvironment;
        this.converters = converters;
        this.fieldType = env.getFieldType();
        this.parentType = (GraphQLNamedType) env.getParentType();
        this.graphQLSchema = env.getGraphQLSchema();
        this.dataFetchingEnvironment = env;
        this.derivedTypes = derivedTypes;
        this.arguments = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T, S> S convertOutput(T output, AnnotatedElement element, AnnotatedType type) {
        if (output == null) {
            return null;
        }

        // Transparently handle unexpected wrapped results. This enables elegant exception handling, partial results etc.
        if (DataFetcherResult.class.equals(output.getClass()) && !DataFetcherResult.class.equals(resolver.getRawReturnType())) {
            DataFetcherResult<?> result = (DataFetcherResult<?>) output;
            if (result.getData() != null) {
                Object convertedData = convert(result.getData(), element, type);
                return (S) DataFetcherResult.newResult()
                        .data(convertedData)
                        .errors(result.getErrors())
                        .localContext(result.getLocalContext())
                        .mapRelativeErrors(result.isMapRelativeErrors())
                        .build();
            }
        }

        return convert(output, element, type);
    }

    @SuppressWarnings("unchecked")
    private <T, S> S convert(T output, AnnotatedElement element, AnnotatedType type) {
        OutputConverter<T, S> outputConverter = converters.getOutputConverter(element, type);
        return outputConverter == null ? (S) output : outputConverter.convertOutput(output, type, this);
    }

    public AnnotatedType getDerived(AnnotatedType type, int index) {
        try {
            return getDerived(type).get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException(String.format("No type derived from %s found at index %d. " +
                            "Make sure the converter implements %s and provides the derived types correctly. " +
                            "See %s for details and possible solutions.",
                    type.getType().getTypeName(), index, DelegatingOutputConverter.class.getSimpleName(), Urls.Errors.DERIVED_TYPES), e);
        }
    }

    public List<AnnotatedType> getDerived(AnnotatedType type) {
        return derivedTypes.getDerived(type);
    }

    public Object getInputValue(Object input, OperationArgument argument) {
        boolean argValuePresent = dataFetchingEnvironment.containsArgument(argument.getName());
        ArgumentInjectorParams params = new ArgumentInjectorParams(input, argValuePresent, argument.getJavaType(), argument.getBaseType(), argument.getParameter(), this);
        Object value = this.globalEnvironment.injectors.getInjector(argument.getJavaType(), argument.getParameter()).getArgumentValue(params);
        if (argValuePresent) {
            arguments.put(argument.getName(), value);
        }
        return value;
    }

    public Directives getDirectives(ExecutionStepInfo step) {
        return new Directives(dataFetchingEnvironment, step);
    }

    public Directives getDirectives() {
        return getDirectives(null);
    }
}
