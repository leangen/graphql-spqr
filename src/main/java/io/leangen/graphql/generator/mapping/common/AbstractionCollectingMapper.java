package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Scalars;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class AbstractionCollectingMapper implements TypeMapper {
    
    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        registerAbstract(javaType, abstractTypes, buildContext);
        return graphQLType(javaType, abstractTypes, operationMapper, buildContext);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        registerAbstract(javaType, abstractTypes, buildContext);
        return graphQLInputType(javaType, abstractTypes, operationMapper, buildContext);
    }

    protected abstract GraphQLOutputType graphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext);
    
    protected abstract GraphQLInputType graphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext);
    
    protected void registerAbstract(AnnotatedType type, Set<Type> abstractTypes, BuildContext buildContext) {
        if (ClassUtils.isAbstract(type)) {
            abstractTypes.add(type.getType());
        }
    }

    protected Set<Type> collectAbstract(AnnotatedType javaType, Set<Type> seen, BuildContext buildContext) {
        javaType = buildContext.globalEnvironment.getMappableType(javaType);
        if (Scalars.isScalar(javaType.getType())) {
            return Collections.emptySet();
        }
        if (GenericTypeReflector.isSuperType(Collection.class, javaType.getType())) {
            AnnotatedType elementType = GenericTypeReflector.getTypeParameter(javaType, Collection.class.getTypeParameters()[0]);
            return collectAbstractInner(elementType, seen, buildContext);
        }
        if (GenericTypeReflector.isSuperType(Map.class, javaType.getType())) {
            AnnotatedType keyType = GenericTypeReflector.getTypeParameter(javaType, Map.class.getTypeParameters()[0]);
            AnnotatedType valueType = GenericTypeReflector.getTypeParameter(javaType, Map.class.getTypeParameters()[1]);
            Set<Type> abstractTypes = collectAbstractInner(keyType, seen, buildContext);
            abstractTypes.addAll(collectAbstractInner(valueType, seen, buildContext));
            return abstractTypes;
        }
        return collectAbstractInner(javaType, seen, buildContext);
    }

    private Set<Type> collectAbstractInner(AnnotatedType javaType, Set<Type> seen, BuildContext buildContext) {
        if (buildContext.abstractComponentTypes.containsKey(javaType.getType())) {
            return buildContext.abstractComponentTypes.get(javaType.getType());
        }
        if (seen.contains(javaType.getType())) {
            return Collections.emptySet();
        }
        seen.add(javaType.getType());
        Set<Type> abstractTypes = new HashSet<>();
        if (ClassUtils.isAbstract(javaType)) {
            abstractTypes.add(javaType.getType());
        }
        buildContext.inputFieldStrategy.getInputFields(javaType)
                .forEach(childQuery -> abstractTypes.addAll(collectAbstract(childQuery.getJavaType(), seen, buildContext)));
        buildContext.abstractComponentTypes.put(javaType.getType(), abstractTypes);
        return abstractTypes;
    }
}
