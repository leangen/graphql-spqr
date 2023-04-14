package io.leangen.graphql.module.common.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonArrayAdapter extends AbstractTypeSubstitutingMapper<List<JsonNode>>
        implements InputConverter<ArrayNode, List<JsonNode>>, DelegatingOutputConverter<ArrayNode, List> {

    private static final AnnotatedType JSON = GenericTypeReflector.annotate(JsonNode.class);

    @Override
    public ArrayNode convertInput(List<JsonNode> substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        return new ArrayNode(JsonNodeFactory.instance, substitute);
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return GenericTypeReflector.annotate(new TypeToken<List<JsonNode>>(){}.getType(), original.getAnnotations());
    }

    @Override
    public List convertOutput(ArrayNode original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        List<Object> nodes = new ArrayList<>(original.size());
        for (JsonNode jsonNode : original) {
            nodes.add(resolutionEnvironment.convertOutput(jsonNode, resolutionEnvironment.resolver.getTypedElement(), JSON));
        }
        return nodes;
    }

    @Override
    public List<AnnotatedType> getDerivedTypes(AnnotatedType type) {
        return Collections.singletonList(JSON);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isSuperClass(ArrayNode.class, type);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return supports(type);
    }
}
