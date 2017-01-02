package io.leangen.graphql.metadata.strategy.input;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.leangen.graphql.metadata.QueryArgument;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class GsonInputDeserializerFactory implements InputDeserializerFactory {

    private final FieldNamingStrategy fieldNamingStrategy;
    private final BiConsumer<GsonBuilder, List<QueryArgument>> configurer;
    private final InputDeserializer defaultInputDeserializer;

    public GsonInputDeserializerFactory() {
        this(new GsonFieldNamingStrategy(), ((gsonBuilder, queryArguments) -> {}));
    }

    public GsonInputDeserializerFactory(FieldNamingStrategy fieldNamingStrategy, BiConsumer<GsonBuilder, List<QueryArgument>> configurer) {
        this.fieldNamingStrategy = fieldNamingStrategy;
        this.configurer = configurer;
        this.defaultInputDeserializer = new GsonInputDeserializer(new GsonBuilder().setFieldNamingStrategy(fieldNamingStrategy).create());
    }

    @Override
    public InputDeserializer getDeserializer(List<QueryArgument> arguments) {
        List<AnnotatedType> types = arguments.stream().map(QueryArgument::getJavaType).collect(Collectors.toList());
        if (types.stream().noneMatch(ClassUtils::isAbstract)) {
            return defaultInputDeserializer;
        }
        
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setFieldNamingStrategy(fieldNamingStrategy);
        configurer.accept(gsonBuilder, arguments);

        types.stream()
                .map(type -> ClassUtils.getRawType(type.getType()))
                .filter(ClassUtils::isAbstract)
                .distinct()
                .map(this::adapterFor)
                .forEach(gsonBuilder::registerTypeAdapterFactory);
        return new GsonInputDeserializer(gsonBuilder.create());
    }

    @SuppressWarnings("unchecked")
    private TypeAdapterFactory adapterFor(Class superClass) {
        RuntimeTypeAdapterFactory adapterFactory = RuntimeTypeAdapterFactory.of(superClass, "_type_");

        ClassUtils.findImplementations(superClass).stream()
                .filter(impl -> !ClassUtils.isAbstract(impl))
                .forEach(adapterFactory::registerSubtype);

        return adapterFactory;
    }
}
