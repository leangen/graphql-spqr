package io.leangen.graphql.metadata.strategy.value.jackson;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class ClassWithOneFieldWithADescription {
    @GraphQLInputField(name = "field", description = "fancy field description")
    private String field;
}

@Getter
@Setter
class ClassWithOneFieldWithAMultilineDescription {
    @GraphQLInputField(name = "field", description = "fancy field description\nline two of fancy field description")
    private String field;
}

@Getter
@Setter
class ClassWithOneFieldWithoutDescription {
    @GraphQLInputField(name = "field")
    private String field;
}

public class JacksonValueMapperTest {
    private JacksonValueMapper jacksonValueMapper;

    @Before
    public void before() {
        jacksonValueMapper = new JacksonValueMapper(new ObjectMapper());
    }

    private InputFieldBuilderParams generateInputFieldBuilderParamsForClass(Class<?> clazz) {
        return InputFieldBuilderParams.builder()
                .withType(GenericTypeReflector.annotate(clazz))
                .withEnvironment(GlobalEnvironment.EMPTY)
                .withConcreteSubTypes(Collections.emptyList())
                .build();
    }

    @Test
    public void fetchFieldsDescriptionFromAnnotation() {
        // given
        final String EXPECTED_DESCRIPTION = "fancy field description";
        final InputFieldBuilderParams inputFieldBuilderParams = generateInputFieldBuilderParamsForClass(
                ClassWithOneFieldWithADescription.class);

        // when
        InputField inputField = jacksonValueMapper.getInputFields(inputFieldBuilderParams).iterator().next();

        // then
        assertEquals(EXPECTED_DESCRIPTION, inputField.getDescription());
    }

    @Test
    public void fetchFieldsMultilineDescriptionFromAnnotation() {
        // given
        final String EXPECTED_DESCRIPTION = "fancy field description\nline two of fancy field description";
        final InputFieldBuilderParams inputFieldBuilderParams = generateInputFieldBuilderParamsForClass(
                ClassWithOneFieldWithAMultilineDescription.class);

        // when
        InputField inputField = jacksonValueMapper.getInputFields(inputFieldBuilderParams).iterator().next();

        // then
        assertEquals(EXPECTED_DESCRIPTION, inputField.getDescription());
    }

    @Test
    public void fetchFieldWithNoDescriptionInAnnotation() {
        // when
        final InputFieldBuilderParams inputFieldBuilderParams = generateInputFieldBuilderParamsForClass(
                ClassWithOneFieldWithoutDescription.class);
        InputField inputField = jacksonValueMapper.getInputFields(inputFieldBuilderParams).iterator().next();

        // then
        assertNull(inputField.getDescription());

    }
}