package io.leangen.graphql;

import graphql.relay.Relay;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.TypeRegistry;
import io.leangen.graphql.generator.mapping.ArgumentInjectorRegistry;
import io.leangen.graphql.generator.mapping.ConverterRegistry;
import io.leangen.graphql.metadata.messages.EmptyMessageBundle;

import java.util.Collections;

class TestGlobalEnvironment extends GlobalEnvironment {

    TestGlobalEnvironment() {
        super(
                EmptyMessageBundle.instance,
                new Relay(),
                new TypeRegistry(Collections.emptySet()),
                new ConverterRegistry(Collections.emptyList(), Collections.emptyList()),
                new ArgumentInjectorRegistry(Collections.emptyList())
        );
    }
}
