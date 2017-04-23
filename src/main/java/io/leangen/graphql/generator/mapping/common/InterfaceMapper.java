package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.generator.types.MappedGraphQLInterfaceType;
import io.leangen.graphql.util.ClassUtils;

import static graphql.schema.GraphQLInterfaceType.newInterface;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InterfaceMapper extends CachingMapper<GraphQLInterfaceType, GraphQLInputObjectType> {

    private final InterfaceMappingStrategy interfaceStrategy;
    private final ObjectTypeMapper objectTypeMapper;

    public InterfaceMapper(InterfaceMappingStrategy interfaceStrategy, ObjectTypeMapper objectTypeMapper) {
        this.interfaceStrategy = interfaceStrategy;
        this.objectTypeMapper = objectTypeMapper;
    }

    @Override
    public GraphQLInterfaceType toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInterfaceType.Builder typeBuilder = newInterface()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType));

        List<GraphQLFieldDefinition> fields = objectTypeMapper.getFields(javaType, buildContext, operationMapper);
        fields.forEach(typeBuilder::field);

        typeBuilder.typeResolver(buildContext.typeResolver);
        GraphQLInterfaceType type = new MappedGraphQLInterfaceType(typeBuilder.build(), javaType);

        if (javaType.isAnnotationPresent(GraphQLInterface.class)) {
            GraphQLInterface meta = javaType.getAnnotation(GraphQLInterface.class);
            if (meta.implementationAutoDiscovery()) {
                ClassUtils.findImplementations(javaType, meta.scanPackages()).forEach(impl ->
                        getImplementingType(impl, abstractTypes, operationMapper, buildContext)
                                .ifPresent(implType -> buildContext.typeRepository.registerCovariantTypes(type.getName(), impl, implType)));

            }
        }
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return objectTypeMapper.toGraphQLInputType(typeName, javaType, abstractTypes, operationMapper, buildContext);
    }
    
    @Override
    public boolean supports(AnnotatedType type) {
        return interfaceStrategy.supports(type);
    }

    private Optional<GraphQLObjectType> getImplementingType(AnnotatedType implType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return Optional.of(implType)
                .filter(impl -> !GenericTypeReflector.isMissingTypeParameters(impl.getType()))
                .filter(impl -> !interfaceStrategy.supports(impl))
                .map(impl -> operationMapper.toGraphQLType(impl, abstractTypes, buildContext))
                .filter(impl -> impl instanceof GraphQLObjectType)
                .map(impl -> (GraphQLObjectType) impl);
    }
}
