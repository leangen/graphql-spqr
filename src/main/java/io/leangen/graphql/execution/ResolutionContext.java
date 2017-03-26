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
public class ResolutionContext {

    public final Object source;
    public final Object context;
    public final ValueMapper valueMapper;
    public final ConnectionRequest connectionRequest;
    public final GlobalContext globalContext;
    public final List<Field> fields;
    public final GraphQLOutputType fieldType;
    public final GraphQLType parentType;
    public final GraphQLSchema graphQLSchema;

    public ResolutionContext(DataFetchingEnvironment env, ConnectionRequest connectionRequest, ValueMapper valueMapper, GlobalContext globalContext) {
        
        this.source = env.getSource();
        this.context = env.getContext();
        this.valueMapper = valueMapper;
        this.connectionRequest = connectionRequest;
        this.globalContext = globalContext;
        this.fields = env.getFields();
        this.fieldType = env.getFieldType();
        this.parentType = env.getParentType();
        this.graphQLSchema = env.getGraphQLSchema();
    }

    @SuppressWarnings("unchecked")
    public Object convertOutput(Object output, AnnotatedType type) {
        OutputConverter outputConverter = this.globalContext.converters.getOutputConverter(type);
        return outputConverter == null ? output : outputConverter.convertOutput(output, type, this);
    }

    @SuppressWarnings("unchecked")
    public Object convertInput(Object input, AnnotatedType type, ResolutionContext resolutionContext) {
        InputConverter inputConverter = this.globalContext.converters.getInputConverter(type);
        return inputConverter == null ? input : inputConverter.convertInput(input, type, resolutionContext);
    }

    public Object getInputValue(Object input, AnnotatedType type) {
        Object in = this.globalContext.injectors.getInjector(type).getArgumentValue(input, type, this);
        return convertInput(in, type, this);
    }
    
    public AnnotatedType getMappableType(AnnotatedType type) {
        return this.globalContext.converters.getMappableType(type);
    }
}
