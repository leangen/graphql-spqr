package io.leangen.graphql;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SubscriptionTest {

    @Test
    public void subscriptionTest() throws InterruptedException {

        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new Ticker())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();

        ExecutionResult res = exe.execute("subscription Tick { tick }");
        Publisher<ExecutionResult> stream = res.getData();
        //doesn't actually need to be atomic, but needs to be effectively final yet mutable
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean complete = new AtomicBoolean(false);
        stream.subscribe(new Subscriber<ExecutionResult>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(10);
            }

            @Override
            public void onNext(ExecutionResult executionResult) {
                counter.getAndIncrement();
            }

            @Override
            public void onError(Throwable throwable) {
                fail();
            }

            @Override
            public void onComplete() {
                complete.set(true);
            }
        });
        assertTrue(complete.get());
        assertEquals(2, counter.get());
    }

    public static class Ticker {

        @GraphQLSubscription
        public Publisher<Integer> tick() {
            Observable<Integer> observable = Observable.create(emitter -> {
                emitter.onNext(1);
                Thread.sleep(1000);
                emitter.onNext(2);
                Thread.sleep(1000);
                emitter.onComplete();
            });

            return observable.toFlowable(BackpressureStrategy.BUFFER);
        }
    }
}
