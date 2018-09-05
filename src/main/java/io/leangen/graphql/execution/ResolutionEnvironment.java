package io.leangen.graphql.execution;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

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
    public final ValueMapper valueMapper;
    public final GlobalEnvironment globalEnvironment;
    public final List<Field> fields;
    public final GraphQLOutputType fieldType;
    public final GraphQLType parentType;
    public final GraphQLSchema graphQLSchema;
    public final DataFetchingEnvironment dataFetchingEnvironment;
    public final Map<String, Object> arguments;

    public ResolutionEnvironment(DataFetchingEnvironment env, ValueMapper valueMapper, GlobalEnvironment globalEnvironment) {
        
        this.context = env.getSource();
        this.rootContext = env.getContext();
        this.valueMapper = valueMapper;
        this.globalEnvironment = globalEnvironment;
        this.fields = env.getFields();
        this.fieldType = env.getFieldType();
        this.parentType = env.getParentType();
        this.graphQLSchema = env.getGraphQLSchema();
        this.dataFetchingEnvironment = env;
        this.arguments = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T, S> S convertOutput(T output, AnnotatedType type) {
        if (output == null) {
            return null;
        }
        OutputConverter<T, S> outputConverter = this.globalEnvironment.converters.getOutputConverter(type);
        return outputConverter == null ? (S) output : outputConverter.convertOutput(output, type, this);
    }

    public Object getInputValue(Object input, OperationArgument argument) {
        ArgumentInjectorParams params = new ArgumentInjectorParams(input, argument.getJavaType(), argument.getParameter(), this);
        Object value = this.globalEnvironment.injectors.getInjector(argument.getJavaType(), argument.getParameter()).getArgumentValue(params);
        if (dataFetchingEnvironment.containsArgument(argument.getName())) {
            arguments.put(argument.getName(), value);
        }
        return value;
    }
}
