package io.leangen.graphql.generator.mapping.core;

import graphql.ExceptionWhileDataFetching;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionStepInfo;
import graphql.language.OperationDefinition;
import graphql.schema.*;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.SchemaTransformer;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.util.ClassUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PublisherAdapter<T> extends AbstractTypeSubstitutingMapper<Object> implements SchemaTransformer, OutputConverter<Publisher<T>, Object> {

    private final Executor executor;

    public PublisherAdapter() {
        this(Runnable::run); //Run on the caller thread
    }

    @SuppressWarnings("WeakerAccess")
    public PublisherAdapter(Executor executor) {
        this.executor = executor;
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        throw new UnsupportedOperationException(ClassUtils.getRawType(javaType.getType()).getSimpleName() + " can not be used as an input type");
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType innerType = GenericTypeReflector.getTypeParameter(original, Publisher.class.getTypeParameters()[0]);
        return TypeFactory.parameterizedAnnotatedClass(List.class, original.getAnnotations(), innerType);
    }

    @Override
    public GraphQLFieldDefinition transformField(GraphQLFieldDefinition field, Operation operation, OperationMapper operationMapper, BuildContext buildContext) {
        //Publisher returned from a subscription must be mapped as a singular result (i.e. not a list)
        if (operation.getOperationType() == OperationDefinition.Operation.SUBSCRIPTION) {
            return field.transform(builder -> builder.type(unwrapList(field.getType())));
        }
        //In other operations, a Publisher is effectively equivalent to a list
        return field;
    }

    @Override
    public Object convertOutput(Publisher<T> original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        //Subscriptions are expected to return a Publisher directly, so no conversion needed
        if (resolutionEnvironment.isSubscription()) {
            return original;
        }
        //Otherwise, convert the Publisher into a CompletableFuture
        return convertOutputForNonSubscription(original, type, resolutionEnvironment);
    }

    @SuppressWarnings("WeakerAccess")
    protected Object convertOutputForNonSubscription(Publisher<T> original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return collect(original, resolutionEnvironment.getDataFetchingEnvironment().getExecutionStepInfo());
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return ClassUtils.isSuperClass(Publisher.class, type);
    }

    private <R> CompletableFuture<DataFetcherResult<List<R>>> collect(Publisher<R> publisher, ExecutionStepInfo step) {
        CompletableFuture<DataFetcherResult<List<R>>> promise = new CompletableFuture<>();

        executor.execute(() -> publisher.subscribe(new Subscriber<>() {

            private final List<R> buffer = new ArrayList<>();

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(R result) {
                buffer.add(result);
            }

            @Override
            public void onError(Throwable error) {
                ExceptionWhileDataFetching wrapped = new ExceptionWhileDataFetching(step.getPath(), error, step.getField().getSingleField().getSourceLocation());
                promise.complete(DataFetcherResult.<List<R>>newResult()
                        .data(buffer)
                        .error(wrapped)
                        .build());
            }

            @Override
            public void onComplete() {
                promise.complete(DataFetcherResult.<List<R>>newResult().data(buffer).build());
            }
        }));
        return promise;
    }

    private GraphQLOutputType unwrapList(GraphQLOutputType type) {
        if (type instanceof GraphQLNonNull) {
            GraphQLType wrapped = ((GraphQLNonNull) type).getWrappedType();
            if (wrapped instanceof GraphQLList) {
                return (GraphQLOutputType) ((GraphQLList) wrapped).getWrappedType();
            }
        } else if (type instanceof GraphQLList) {
            return (GraphQLOutputType) ((GraphQLList) type).getWrappedType();
        }
        return type;
    }
}
