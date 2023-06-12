package io.leangen.graphql.generator.mapping.core;

import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.schema.GraphQLInputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.*;
import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DataFetcherResultAdapter<T> extends AbstractTypeSubstitutingMapper<T> implements
        DelegatingOutputConverter<DataFetcherResult<T>, DataFetcherResult<?>>, ResolverInterceptorFactory {

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        throw new UnsupportedOperationException(DataFetcherResult.class.getSimpleName() + " can not be used as an input type");
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType innerType = GenericTypeReflector.getTypeParameter(original, DataFetcherResult.class.getTypeParameters()[0]);
        return ClassUtils.addAnnotations(innerType, original.getAnnotations());
    }


    @Override
    public DataFetcherResult<?> convertOutput(DataFetcherResult<T> original, AnnotatedType type, ResolutionEnvironment env) {
        if (original.getData() == null && env.errors.isEmpty()) {
            return original;
        }
        return original.transform(res -> res
                .data(original.getData() != null
                        ? env.convertOutput(original.getData(), env.resolver.getTypedElement(), env.getDerived(type, 0))
                        : null)
                .errors(env.errors));
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return ClassUtils.isSuperClass(DataFetcherResult.class, type);
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
    public List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params) {
        return Collections.emptyList();
    }

    @Override
    public List<ResolverInterceptor> getOuterInterceptors(ResolverInterceptorFactoryParams params) {
        return Collections.singletonList(new ErrorAppender());
    }

    private static class ErrorAppender implements ResolverInterceptor {
        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
            Object result = continuation.proceed(context);
            List<GraphQLError> errors = context.getResolutionEnvironment().errors;
            if (!errors.isEmpty()) {
                if (result == null) {
                    return DataFetcherResult.newResult().errors(errors).build();
                }
                if (result.getClass() == DataFetcherResult.class) {
                    return ((DataFetcherResult<?>) result).transform(res -> res.errors(errors));
                }
                return DataFetcherResult.newResult().data(result).errors(errors).build();
            }
            return result;
        }
    }
}
