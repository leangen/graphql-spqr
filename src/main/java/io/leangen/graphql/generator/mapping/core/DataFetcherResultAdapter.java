package io.leangen.graphql.generator.mapping.core;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
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
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

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
        @SuppressWarnings("unchecked")
        public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
            Object result = continuation.proceed(context);
            ResolutionEnvironment env = context.getResolutionEnvironment();
            List<GraphQLError> errors = env.errors;
            if (errors.isEmpty()) {
                return result;
            }
            if (env.resolver.isBatched()) {
                Objects.requireNonNull(env.batchLoaderEnvironment, "Batch loader invoked in a non-batched environment");
                return ((CompletionStage<List<Object>>) result).thenApply(list -> {
                            List<Object> remapped = new ArrayList<>(list.size());
                            for (int i = 0; i < list.size(); i++) {
                                Object keyContext = env.batchLoaderEnvironment.getKeyContextsList().get(i);
                                if (keyContext instanceof DataFetchingEnvironment) {
                                    remapped.add(appendErrors(list.get(i), bindErrors(errors, (DataFetchingEnvironment) keyContext)));
                                } else {
                                    remapped.add(appendErrors(list.get(i), errors));
                                }
                            }
                            return remapped;
                        }
                );
            }
            return appendErrors(result, errors);
        }
    }

    private static List<GraphQLError> bindErrors(List<GraphQLError> errors, DataFetchingEnvironment env) {
        return errors.stream()
                .map(e -> GraphqlErrorBuilder.newError(env)
                        .errorType(e.getErrorType())
                        .message(e.getMessage())
                        .extensions(e.getExtensions())
                        .build())
                .collect(Collectors.toList());
    }

    private static Object appendErrors(Object result, List<GraphQLError> errors) {
        if (result == null) {
            return DataFetcherResult.newResult().errors(errors).build();
        }
        if (result.getClass() == DataFetcherResult.class) {
            return ((DataFetcherResult<?>) result).transform(res -> res.errors(errors));
        }
        return DataFetcherResult.newResult().data(result).errors(errors).build();
    }
}
