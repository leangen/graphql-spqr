package io.leangen.graphql.generator;

import java.util.List;

import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import io.leangen.graphql.annotations.GraphQLTypeResolver;
import io.leangen.graphql.generator.exceptions.UnresolvableTypeException;
import io.leangen.graphql.metadata.strategy.type.TypeMetaDataGenerator;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class HintedTypeResolver implements TypeResolver {

    private final TypeRepository typeRepository;
    private final TypeMetaDataGenerator typeMetaDataGenerator;
    private final String abstractTypeName;

    public HintedTypeResolver(TypeRepository typeRepository, TypeMetaDataGenerator typeMetaDataGenerator) {
        this(null, typeRepository, typeMetaDataGenerator);
    }

    public HintedTypeResolver(String abstractTypeName, TypeRepository typeRepository, TypeMetaDataGenerator typeMetaDataGenerator) {
        this.typeRepository = typeRepository;
        this.typeMetaDataGenerator = typeMetaDataGenerator;
        this.abstractTypeName = abstractTypeName;
    }
    
    @Override
    public GraphQLObjectType getType(Object result) {
        Class<?> type = result.getClass();

        if (type.isAnnotationPresent(GraphQLTypeResolver.class)) {
            try {
                return type.getAnnotation(GraphQLTypeResolver.class).value().newInstance()
                        .resolveType(typeRepository, typeMetaDataGenerator, result);
            } catch (ReflectiveOperationException e) {
                throw new UnresolvableTypeException(result, e);
            }
        }
        
        List<MappedType> mappedTypes = abstractTypeName == null 
                ? typeRepository.getOutputTypes(type) 
                : typeRepository.getOutputTypes(abstractTypeName, type);
        
        if (mappedTypes.size() == 1) {
            return (GraphQLObjectType) mappedTypes.get(0).graphQLType;
        }
        throw new UnresolvableTypeException(result);
    }
}
