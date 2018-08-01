package io.leangen.graphql;

import graphql.relay.Relay;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.generator.mapping.ArgumentInjectorRepository;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.metadata.messages.EmptyMessageBundle;

import java.util.Collections;

class TestGlobalEnvironment extends GlobalEnvironment {

    TestGlobalEnvironment() {
        super(
                EmptyMessageBundle.instance,
                new Relay(),
                new TypeRepository(Collections.emptySet()),
                new ConverterRepository(Collections.emptyList(), Collections.emptyList()),
                new ArgumentInjectorRepository(Collections.emptyList())
        );
    }
}
