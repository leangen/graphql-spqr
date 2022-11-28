package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static graphql.schema.GraphQLInterfaceType.newInterface;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InterfaceMapper extends CachingMapper<GraphQLInterfaceType, GraphQLInputObjectType> {

    private final InterfaceMappingStrategy interfaceStrategy;
    private final ObjectTypeMapper objectTypeMapper;

    public InterfaceMapper(InterfaceMappingStrategy interfaceStrategy, ObjectTypeMapper objectTypeMapper) {
        this.interfaceStrategy = Objects.requireNonNull(interfaceStrategy);
        this.objectTypeMapper = Objects.requireNonNull(objectTypeMapper);
    }

    @Override
    public GraphQLInterfaceType toGraphQLType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        GraphQLInterfaceType.Builder typeBuilder = newInterface()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType, buildContext.messageBundle));

        List<GraphQLFieldDefinition> fields = objectTypeMapper.getFields(typeName, javaType, env);
        fields.forEach(typeBuilder::field);

        buildContext.directiveBuilder.buildInterfaceTypeDirectives(javaType, buildContext.directiveBuilderParams()).forEach(directive ->
                typeBuilder.withAppliedDirective(env.operationMapper.toGraphQLAppliedDirective(directive, buildContext)));
        typeBuilder.comparatorRegistry(buildContext.comparatorRegistry(javaType));
        GraphQLInterfaceType type = typeBuilder.build();
        buildContext.codeRegistry.typeResolver(type, buildContext.typeResolver);

        registerImplementations(javaType, type, env);
        buildContext.typeRegistry.registerMapping(type.getName(), javaType);
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        return objectTypeMapper.toGraphQLInputType(typeName, javaType, env);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return interfaceStrategy.supports(type);
    }

    private void registerImplementations(AnnotatedType javaType, GraphQLInterfaceType type, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;
        buildContext.implDiscoveryStrategy.findImplementations(javaType, isImplementationAutoDiscoveryEnabled(javaType), getScanPackages(javaType), buildContext)
                .forEach(impl -> getImplementingType(impl, env)
                        .ifPresent(implType -> buildContext.typeRegistry.registerDiscoveredCovariantType(type.getName(), impl, implType)));
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean isImplementationAutoDiscoveryEnabled(AnnotatedType javaType) {
        return javaType.isAnnotationPresent(GraphQLInterface.class) && javaType.getAnnotation(GraphQLInterface.class).implementationAutoDiscovery();
    }

    @SuppressWarnings("WeakerAccess")
    protected String[] getScanPackages(AnnotatedType javaType) {
        return javaType.isAnnotationPresent(GraphQLInterface.class) ? javaType.getAnnotation(GraphQLInterface.class).scanPackages() : Utils.emptyArray();
    }

    private Optional<GraphQLObjectType> getImplementingType(AnnotatedType implType, TypeMappingEnvironment env) {
        return Optional.of(implType)
                .filter(impl -> !interfaceStrategy.supports(impl))
                .map(impl -> env.operationMapper.toGraphQLType(impl, env))
                .filter(impl -> impl instanceof GraphQLObjectType)
                .map(impl -> (GraphQLObjectType) impl);
    }
}
