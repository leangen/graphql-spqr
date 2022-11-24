package io.leangen.graphql;

import graphql.relay.Relay;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.TypeRegistry;
import io.leangen.graphql.generator.mapping.ArgumentInjectorRegistry;
import io.leangen.graphql.generator.mapping.ConverterRegistry;
import io.leangen.graphql.metadata.messages.EmptyMessageBundle;
import io.leangen.graphql.metadata.strategy.DefaultInclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeTransformer;

import java.util.Collections;

class TestGlobalEnvironment extends GlobalEnvironment {

    TestGlobalEnvironment() {
        super(
                EmptyMessageBundle.INSTANCE,
                new Relay(),
                new TypeRegistry(Collections.emptyMap()),
                new ConverterRegistry(Collections.emptyList(), Collections.emptyList()),
                new ArgumentInjectorRegistry(Collections.emptyList()),
                new DefaultTypeTransformer(false, false),
                new DefaultInclusionStrategy(),
                new DefaultTypeInfoGenerator());
    }
}
