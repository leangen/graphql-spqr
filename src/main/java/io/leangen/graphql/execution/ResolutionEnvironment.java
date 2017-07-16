package io.leangen.graphql.execution;

import java.lang.reflect.AnnotatedType;
import java.util.List;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

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
    }

    @SuppressWarnings("unchecked")
    public Object convertOutput(Object output, AnnotatedType type) {
        if (output == null) {
            return null;
        }
        OutputConverter outputConverter = this.globalEnvironment.converters.getOutputConverter(type);
        return outputConverter == null ? output : outputConverter.convertOutput(output, type, this);
    }

    @SuppressWarnings("unchecked")
    public Object convertInput(Object input, AnnotatedType type) {
        if (input == null) {
            return null;
        }
        InputConverter inputConverter = this.globalEnvironment.converters.getInputConverter(type);
        return inputConverter == null ? input : inputConverter.convertInput(input, type, this);
    }

    public Object getInputValue(Object input, AnnotatedType type) {
        Object in = this.globalEnvironment.injectors.getInjector(type).getArgumentValue(input, type, this);
        return convertInput(in, type);
    }
    
    public AnnotatedType getMappableType(AnnotatedType type) {
        return this.globalEnvironment.converters.getMappableType(type);
    }
}
