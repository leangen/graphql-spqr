package io.leangen.graphql.generator.mapping.core;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureAdapter<T> extends AbstractTypeSubstitutingMapper
        implements InputConverter<CompletableFuture<T>, T>, DelegatingOutputConverter<CompletableFuture<T>, CompletableFuture<?>> {

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType innerType = GenericTypeReflector.getTypeParameter(original, CompletableFuture.class.getTypeParameters()[0]);
        return ClassUtils.addAnnotations(innerType, original.getAnnotations());
    }

    @Override
    public CompletableFuture<T> convertInput(T substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        return CompletableFuture.completedFuture(substitute);
    }

    @Override
    public CompletableFuture<?> convertOutput(CompletableFuture<T> original, AnnotatedType type, ResolutionEnvironment env) {
        return original.thenApply(res -> env.convertOutput(res, env.resolver.getTypedElement(), env.getDerived(type, 0)));
    }

    @Override
    public List<AnnotatedType> getDerivedTypes(AnnotatedType type) {
        return Collections.singletonList(getSubstituteType(type));
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isSuperClass(CompletableFuture.class, type);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return supports(type);
    }
}
