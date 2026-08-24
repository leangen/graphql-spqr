package io.leangen.graphql.metadata.strategy.value.jackson;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.InputConverter;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.*;

import java.lang.reflect.AnnotatedType;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class ConvertingDeserializers extends Deserializers.Base {

    private final Map<InputConverter, ValueDeserializer> deserializers;

    ConvertingDeserializers(GlobalEnvironment environment, ObjectMapper mapper) {
        this.deserializers = environment.getInputConverters().stream()
                .collect(Collectors.toMap(Function.identity(), converter -> new ConvertingDeserializer(converter, environment, mapper)));
    }

    @Override
    public ValueDeserializer<?> findMapDeserializer(MapType type, DeserializationConfig config,
                                                   BeanDescription.Supplier beanDesc, KeyDeserializer keyDeserializer,
                                                   TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer) {

        return forJavaType(type);
    }

    @Override
    public ValueDeserializer<?> findEnumDeserializer(JavaType type, DeserializationConfig config, BeanDescription.Supplier beanDesc) {
        try {
            return forType(GenericTypeReflector.annotate(type));
        } catch (IllegalArgumentException e) {
            if (type instanceof SimpleType) {
                SimpleType simpleType = (SimpleType) type;
                return forType(GenericTypeReflector.annotate(simpleType.getRawClass()));
            }
            throw e;
        }
    }

    @Override
    public ValueDeserializer<?> findTreeNodeDeserializer(JavaType nodeType, DeserializationConfig config, BeanDescription.Supplier beanDesc) {
        try {
            return forType(GenericTypeReflector.annotate(nodeType));
        } catch (IllegalArgumentException e) {
            if (nodeType instanceof SimpleType) {
                SimpleType simpleType = (SimpleType) nodeType;
                return forType(GenericTypeReflector.annotate(simpleType.getRawClass()));
            }
            throw e;
        }
    }

    @Override
    public ValueDeserializer<?> findReferenceDeserializer(ReferenceType refType, DeserializationConfig config, BeanDescription.Supplier beanDesc, TypeDeserializer contentTypeDeserializer, ValueDeserializer<?> contentDeserializer) {
        return forJavaType(refType);
    }

    @Override
    public ValueDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription.Supplier beanDesc) {
        return forJavaType(type);
    }

    @Override
    public ValueDeserializer<?> findArrayDeserializer(ArrayType type, DeserializationConfig config, BeanDescription.Supplier beanDesc, TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer) {
        return forJavaType(type);
    }

    @Override
    public ValueDeserializer<?> findCollectionDeserializer(CollectionType type, DeserializationConfig config, BeanDescription.Supplier beanDesc, TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer) {
        return forJavaType(type);
    }

    @Override
    public ValueDeserializer<?> findCollectionLikeDeserializer(CollectionLikeType type, DeserializationConfig config, BeanDescription.Supplier beanDesc, TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer) {
        return forJavaType(type);
    }

    @Override
    public ValueDeserializer<?> findMapLikeDeserializer(MapLikeType type, DeserializationConfig config, BeanDescription.Supplier beanDesc, KeyDeserializer keyDeserializer, TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer) {
        return forJavaType(type);
    }

    @Override
    public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueType) {
        TypeFactory typeFactory = TypeFactory.createDefaultInstance();
        JavaType javaType = typeFactory.constructType(valueType);
        return forJavaType(javaType) != null;
    }

    private ValueDeserializer forType(AnnotatedType type) {
        return deserializers.keySet().stream().filter(c -> c.supports(type)).findFirst().map(deserializers::get).orElse(null);
    }

    private ValueDeserializer forJavaType(JavaType type) {
        return forType(TypeUtils.toJavaType(type));
    }
}
