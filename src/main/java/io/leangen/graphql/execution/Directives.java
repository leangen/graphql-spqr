package io.leangen.graphql.execution;

import graphql.execution.ExecutionContext;
import graphql.execution.ValuesResolver;
import graphql.introspection.Introspection;
import graphql.language.Directive;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeTraverser;
import graphql.language.NodeVisitorStub;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLDirective;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Directives {

    private Map<Introspection.DirectiveLocation, Map<String, List<Map<String, Object>>>> directives = new HashMap<>();

    private static final ValuesResolver valuesResolver = new ValuesResolver();

    Directives(DataFetchingEnvironment env) {
        // Field directives
        env.getFields().forEach(field ->
                directives.merge(Introspection.DirectiveLocation.FIELD, parseDirectives(field.getDirectives(), env), (directiveMap1, directiveMap2) -> {
                    directiveMap2.forEach((directiveName, directiveValues) -> directiveMap1.merge(directiveName, directiveValues,
                            (valueList1, valueList2) -> Stream.concat(valueList1.stream(), valueList2.stream()).collect(Collectors.toList()))
                    );
                    return directiveMap1;
                }));

        // Operation directives
        Map<String, List<Map<String, Object>>> operationDirectives = parseDirectives(env.getExecutionContext().getOperationDefinition().getDirectives(), env);
        if (OperationDefinition.Operation.MUTATION.equals(env.getExecutionContext().getOperationDefinition().getOperation())) {
            directives.put(Introspection.DirectiveLocation.MUTATION, operationDirectives);
            directives.put(Introspection.DirectiveLocation.QUERY, Collections.emptyMap());
        } else {
            directives.put(Introspection.DirectiveLocation.QUERY, operationDirectives);
            directives.put(Introspection.DirectiveLocation.MUTATION, Collections.emptyMap());
        }

        // Fragment directives
        if (env.getExecutionStepInfo().hasParent() && env.getExecutionStepInfo().getParent().getField() != null) {
            FragmentDirectiveCollector fragmentDirectiveCollector = FragmentDirectiveCollector.collect(env);
            directives.put(Introspection.DirectiveLocation.INLINE_FRAGMENT, parseDirectives(fragmentDirectiveCollector.getInlineFragmentDirs(), env));
            directives.put(Introspection.DirectiveLocation.FRAGMENT_SPREAD, parseDirectives(fragmentDirectiveCollector.getFragmentDirs(), env));
            directives.put(Introspection.DirectiveLocation.FRAGMENT_DEFINITION, parseDirectives(fragmentDirectiveCollector.getFragmentDefDirs(), env));
        }
    }

    private Map<String, List<Map<String, Object>>> parseDirectives(List<Directive> directives, DataFetchingEnvironment env) {
        return directives.stream()
                .collect(Collectors.toMap(Directive::getName, dir -> {
                    GraphQLDirective directive = env.getExecutionContext().getGraphQLSchema().getDirective(dir.getName());
                    if (directive == null) {
                        return Collections.emptyList();
                    }
                    return Collections.singletonList(Collections.unmodifiableMap(
                            valuesResolver.getArgumentValues(env.getGraphQLSchema().getFieldVisibility(), directive.getArguments(),
                                    dir.getArguments(), env.getExecutionContext().getVariables())));
                }));
    }

    Map<Introspection.DirectiveLocation, Map<String, List<Map<String, Object>>>> getDirectives() {
        return directives;
    }

    private static class FragmentDirectiveCollector extends NodeVisitorStub {

        private final List<Directive> inlineFragmentDirs;
        private final List<Directive> fragmentDirs;
        private final List<Directive> fragmentDefDirs;
        private final ExecutionContext executionContext;
        private final Set<Field> fieldsToFind;

        private FragmentDirectiveCollector(DataFetchingEnvironment env) {
            this.inlineFragmentDirs = new ArrayList<>();
            this.fragmentDirs = new ArrayList<>();
            this.fragmentDefDirs = new ArrayList<>();
            this.executionContext = env.getExecutionContext();
            this.fieldsToFind = new HashSet<>(env.getFields());
        }

        public static FragmentDirectiveCollector collect(DataFetchingEnvironment env) {
            FragmentDirectiveCollector fragmentDirectiveCollector = new FragmentDirectiveCollector(env);
            new NodeTraverser().preOrder(fragmentDirectiveCollector, env.getExecutionStepInfo().getParent().getField().getSelectionSet().getSelections());
            return fragmentDirectiveCollector;
        }

        @Override
        public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
            FragmentDefinition fragment = executionContext.getFragment(node.getName());
            Optional<Field> foundField = fieldsToFind.stream().filter(field -> fragment.getSelectionSet().getSelections().contains(field)).findAny();
            if (foundField.isPresent()) {
                fragmentDirs.addAll(node.getDirectives());
                fragmentDefDirs.addAll(fragment.getDirectives());
                fieldsToFind.remove(foundField.get());
                if (fieldsToFind.isEmpty()) {
                    return TraversalControl.QUIT;
                }
            }
            return TraversalControl.CONTINUE;
        }

        @Override
        public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
            Optional<Field> foundField = fieldsToFind.stream().filter(field -> node.getSelectionSet().getSelections().contains(field)).findAny();
            if (foundField.isPresent()) {
                inlineFragmentDirs.addAll(node.getDirectives());
                fieldsToFind.remove(foundField.get());
                if (fieldsToFind.isEmpty()) {
                    return TraversalControl.QUIT;
                }
            }
            return TraversalControl.CONTINUE;
        }

        List<Directive> getInlineFragmentDirs() {
            return inlineFragmentDirs;
        }

        List<Directive> getFragmentDirs() {
            return fragmentDirs;
        }

        List<Directive> getFragmentDefDirs() {
            return fragmentDefDirs;
        }
    }
}
