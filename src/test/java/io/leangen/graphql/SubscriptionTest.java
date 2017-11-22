package io.leangen.graphql;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;

import static org.junit.Assert.assertEquals;
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
        stream.subscribe(new Subscriber<ExecutionResult>() {
            private long counter = 1L;

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(10);
            }

            @Override
            public void onNext(ExecutionResult executionResult) {
                assertEquals(counter++, (long) executionResult.getData());
            }

            @Override
            public void onError(Throwable throwable) {
                fail();
            }

            @Override
            public void onComplete() {
                assertEquals(3, counter);
            }
        });
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
