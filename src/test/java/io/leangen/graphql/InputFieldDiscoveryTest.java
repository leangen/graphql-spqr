package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapper;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapper;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.junit.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InputFieldDiscoveryTest {

    private JacksonValueMapper jackson = new JacksonValueMapperFactory().getValueMapper(Collections.emptyMap(), ENVIRONMENT);
    private GsonValueMapper gson = new GsonValueMapperFactory().getValueMapper(Collections.emptyMap(), ENVIRONMENT);

    private static final TypedElement IGNORED_TYPE = new TypedElement(GenericTypeReflector.annotate(Object.class), (AnnotatedElement) null);
    private static final GlobalEnvironment ENVIRONMENT = new TestGlobalEnvironment();

    private static final InputField[] expectedDefaultFields = new InputField[] {
            new InputField("field1", null, IGNORED_TYPE, null, null),
            new InputField("field2", null, IGNORED_TYPE, null, null),
            new InputField("field3", null, IGNORED_TYPE, null, null)
    };
    private static final InputField[] expectedFilteredDefaultFields = new InputField[] {expectedDefaultFields[0], expectedDefaultFields[2]};
    private static final InputField[] expectedExplicitFields = new InputField[] {
            new InputField("aaa", "AAA", IGNORED_TYPE, null, "AAAA"),
            new InputField("bbb", "BBB", IGNORED_TYPE, null, 2222),
            new InputField("ccc", "CCC", IGNORED_TYPE, null, 3333)
    };
    private static final InputField[] expectedQueryFields = new InputField[] {
            new InputField("aaa", null, IGNORED_TYPE, null, null),
            new InputField("bbb", null, IGNORED_TYPE, null, null),
            new InputField("ccc", null, IGNORED_TYPE, null, null)
    };
    
    @Test
    public void basicFieldsTest() {
        assertFieldNamesEqual(FieldsOnly.class, expectedDefaultFields);
    }

    @Test
    public void basicGettersTest() {
        assertFieldNamesEqual(GettersOnly.class, expectedDefaultFields);
    }

    @Test
    public void basicSettersTest() {
        assertFieldNamesEqual(SettersOnly.class, expectedDefaultFields);
    }

    @Test
    public void explicitFieldsTest() {
        assertFieldNamesEqual(ExplicitFields.class, expectedExplicitFields);
    }

    @Test
    public void explicitGettersTest() {
        assertFieldNamesEqual(ExplicitGetters.class, expectedExplicitFields);
    }

    @Test
    public void explicitSettersTest() {
        assertFieldNamesEqual(ExplicitSetters.class, expectedExplicitFields);
    }
    
    @Test
    public void queryFieldsTest() {
        assertFieldNamesEqual(QueryFields.class, expectedQueryFields);
    }

    @Test
    public void queryGettersTest() {
        assertFieldNamesEqual(QueryGetters.class, expectedQueryFields);
    }

    @Test
    public void querySettersTest() {
        assertFieldNamesEqual(QuerySetters.class, expectedQueryFields);
    }

    @Test
    public void mixedFieldsTest() {
        assertFieldNamesEqual(MixedFieldsWin.class, expectedExplicitFields);
    }
    
    @Test
    public void mixedGettersTest() {
        assertFieldNamesEqual(MixedGettersWin.class, expectedExplicitFields);
    }

    @Test
    public void mixedSettersTest() {
        assertFieldNamesEqual(MixedSettersWin.class, expectedExplicitFields);
    }

    @Test
    public void conflictingGettersTest() {
        assertFieldNamesEqual(ConflictingGettersWin.class, expectedExplicitFields);
    }

    @Test
    public void conflictingSettersTest() {
        assertFieldNamesEqual(ConflictingSettersWin.class, expectedExplicitFields);
    }

    @Test
    public void allConflictingSettersTest() {
        assertFieldNamesEqual(AllConflictingSettersWin.class, expectedExplicitFields);
    }

    @Test
    public void hiddenSettersTest() {
        assertFieldNamesEqual(HiddenSetters.class, expectedFilteredDefaultFields);
    }

    @Test
    public void hiddenCtorParamsTest() {
        assertFieldNamesEqual(jackson, HiddenCtorParams.class, expectedFilteredDefaultFields);
    }

    @Test
    public void jacksonMergedTypesTest() {
        Set<InputField> jFields = getInputFields(jackson, MergedTypes.class);
        Set<InputField> gFields = getInputFields(gson, MergedTypes.class);

        assertTypesMerged(jFields);
        assertTypesMerged(gFields);

        assertAllFieldsEqual(jFields, gFields);
    }

    private void assertFieldNamesEqual(Class typeToScan, InputField... expectedFields) {
        Set<InputField> jFields = assertFieldNamesEqual(jackson, typeToScan, expectedFields);
        Set<InputField> gFields = assertFieldNamesEqual(gson, typeToScan, expectedFields);

        assertAllFieldsEqual(jFields, gFields);
    }

    private Set<InputField> assertFieldNamesEqual(InputFieldBuilder mapper, Class typeToScan, InputField... templates) {
        Set<InputField> fields = getInputFields(mapper, typeToScan);
        assertEquals(templates.length, fields.size());
        for (InputField template : templates) {
            Optional<InputField> field = fields.stream().filter(input -> input.getName().equals(template.getName())).findFirst();
            assertTrue("Field '" + template.getName() + "' doesn't match between different strategies", field.isPresent());
            assertEquals(template.getDescription(), field.get().getDescription());
            assertEquals(template.getDefaultValue(), field.get().getDefaultValue());
        }
        return fields;
    }

    private void assertTypesMerged(Set<InputField> fields) {
        Optional<InputField> field1 = fields.stream().filter(field -> field.getName().equals("field1")).findFirst();
        Optional<InputField> field2 = fields.stream().filter(field -> field.getName().equals("field2")).findFirst();
        Optional<InputField> field3 = fields.stream().filter(field -> field.getName().equals("field3")).findFirst();
        assertTrue(field1.isPresent() && field2.isPresent() && field3.isPresent());
        AnnotatedType type1 = field1.get().getTypedElement().getJavaType();
        assertTrue(type1.isAnnotationPresent(GraphQLNonNull.class) && type1.isAnnotationPresent(GraphQLId.class));
        AnnotatedType type2 = field2.get().getTypedElement().getJavaType();
        assertTrue(type2.isAnnotationPresent(GraphQLNonNull.class) && type2.isAnnotationPresent(GraphQLId.class));
        AnnotatedType type3 = field3.get().getTypedElement().getJavaType();
        assertTrue(type3.isAnnotationPresent(GraphQLNonNull.class));
        AnnotatedType type31 = ((AnnotatedParameterizedType) type3).getAnnotatedActualTypeArguments()[0];
        assertTrue(type31.isAnnotationPresent(GraphQLNonNull.class) && type31.isAnnotationPresent(GraphQLScalar.class));
    }

    private Set<InputField> getInputFields(InputFieldBuilder mapper, Class typeToScan) {
        return mapper.getInputFields(
                InputFieldBuilderParams.builder()
                        .withType(GenericTypeReflector.annotate(typeToScan))
                        .withEnvironment(ENVIRONMENT)
                        .build());
    }

    private void assertAllFieldsEqual(Set<InputField> fields1, Set<InputField> fields2) {
        assertEquals(fields1.size(), fields2.size());
        fields1.forEach(f1 -> assertTrue(fields2.stream().anyMatch(f2 -> f1.getName().equals(f2.getName())
                        && Objects.equals(f1.getDescription(), f2.getDescription())
                        && GenericTypeReflector.equals(f1.getJavaType(), f2.getJavaType())
                        && Objects.equals(f1.getDefaultValue(), f2.getDefaultValue()))));
    }

    private class FieldsOnly {
        public String field1;
        public int field2;
        public Object field3;
    }

    private class GettersOnly {
        private String field1;
        private int field2;
        private Object field3;

        public String getField1() {
            return field1;
        }

        public int getField2() {
            return field2;
        }

        public Object getField3() {
            return field3;
        }
    }

    private class SettersOnly {
        private String field1;
        private int field2;
        private Object field3;

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public void setField2(int field2) {
            this.field2 = field2;
        }

        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class ExplicitFields {
        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        public String field1;
        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        public int field2;
        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        public Object field3;
    }

    private class ExplicitGetters {
        private String field1;
        private int field2;
        private Object field3;

        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        public String getField1() {
            return field1;
        }

        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        public int getField2() {
            return field2;
        }

        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        public Object getField3() {
            return field3;
        }
    }

    private class ExplicitSetters {
        private String field1;
        private int field2;
        private Object field3;

        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }
    
    private class QueryFields {
        @GraphQLQuery(name = "aaa")
        public String field1;
        @GraphQLQuery(name = "bbb")
        public int field2;
        @GraphQLQuery(name = "ccc")
        public Object field3;
    }

    private class QueryGetters {
        private String field1;
        private int field2;
        private Object field3;

        @GraphQLQuery(name = "aaa")
        public String getField1() {
            return field1;
        }

        @GraphQLQuery(name = "bbb")
        public int getField2() {
            return field2;
        }

        @GraphQLQuery(name = "ccc")
        public Object getField3() {
            return field3;
        }
    }

    private class QuerySetters {
        private String field1;
        private int field2;
        private Object field3;

        @GraphQLQuery(name = "aaa")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLQuery(name = "bbb")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @GraphQLQuery(name = "ccc")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class MixedFieldsWin {
        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        private String field1;
        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        private int field2;
        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        private Object field3;

        @GraphQLQuery(name = "xxx")
        public String getField1() {
            return field1;
        }

        @GraphQLQuery(name = "yyy")
        public int getField2() {
            return field2;
        }

        @GraphQLQuery(name = "zzz")
        public Object getField3() {
            return field3;
        }
    }
    
    private class MixedGettersWin {
        @GraphQLQuery(name = "xxx")
        private String field1;
        @GraphQLQuery(name = "yyy")
        private int field2;
        @GraphQLQuery(name = "zzz")
        private Object field3;

        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        public String getField1() {
            return field1;
        }

        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        public int getField2() {
            return field2;
        }

        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        public Object getField3() {
            return field3;
        }
    }

    private class MixedSettersWin {
        @GraphQLQuery(name = "xxx")
        private String field1;
        @GraphQLQuery(name = "yyy")
        private int field2;
        @GraphQLQuery(name = "zzz")
        private Object field3;

        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class ConflictingGettersWin {
        @GraphQLInputField(name = "xxx", description = "XXX", defaultValue = "XXXX")
        private String field1;
        @GraphQLInputField(name = "yyy", description = "YYY", defaultValue = "-1")
        private int field2;
        @GraphQLInputField(name = "zzz", description = "ZZZ", defaultValue = "-1")
        private Object field3;

        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        public String getField1() {
            return field1;
        }

        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        public int getField2() {
            return field2;
        }

        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        public Object getField3() {
            return field3;
        }
    }
    
    private class ConflictingSettersWin {
        @GraphQLInputField(name = "xxx", description = "XXX", defaultValue = "XXXX")
        private String field1;
        @GraphQLInputField(name = "yyy", description = "YYY", defaultValue = "-1")
        private int field2;
        @GraphQLInputField(name = "zzz", description = "ZZZ", defaultValue = "-1")
        private Object field3;

        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class AllConflictingSettersWin {
        @GraphQLInputField(name = "xxx", description = "XXX", defaultValue = "XXXX")
        private String field1;
        @GraphQLInputField(name = "yyy", description = "YYY", defaultValue = "-1")
        private int field2;
        @GraphQLInputField(name = "zzz", description = "ZZZ", defaultValue = "-1")
        private Object field3;

        @GraphQLInputField(name = "111", description = "1111", defaultValue = "XXXX")
        public String getField1() {
            return field1;
        }

        @GraphQLInputField(name = "222", description = "2222", defaultValue = "-1")
        public int getField2() {
            return field2;
        }

        @GraphQLInputField(name = "333", description = "3333", defaultValue = "-1")
        public Object getField3() {
            return field3;
        }

        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class HiddenSetters {
        private String field1;
        private int field2;
        private Object field3;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLQuery(name = "ignored")
        @GraphQLInputField(name = "ignored")
        public int getField2() {
            return field2;
        }

        @GraphQLIgnore
        @GraphQLInputField(name = "ignored")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        public Object getField3() {
            return field3;
        }

        @GraphQLInputField
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    public static class HiddenCtorParams {
        private String field1;
        private int field2;
        private Object field3;

        @JsonCreator
        public HiddenCtorParams(String field1, @GraphQLIgnore int field2, Object field3) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
        }
    }

    private static class MergedTypes {
        private @GraphQLNonNull String field1;
        private RelayTest.Book field2;
        private List<RelayTest.@GraphQLNonNull Book> field3;

        @JsonCreator
        // Only Jackson will pick this up. Gson does not use the constructor.
        public MergedTypes(@GraphQLId String field1, @GraphQLId RelayTest.Book field2, List<RelayTest.@GraphQLScalar Book> field3) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
        }

        public String getField1() {
            return field1;
        }

        // Only Gson will pick this up. In Jackson, the constructor parameter shadows the unannotated setter.
        public void setField1(@GraphQLId String field1) {
            this.field1 = field1;
        }

        @GraphQLInputField
        public RelayTest.@GraphQLNonNull Book getField2() {
            return field2;
        }

        // Only Gson will pick this up. In Jackson, the constructor parameter shadows the unannotated setter.
        public void setField2(RelayTest.@GraphQLId Book field2) {
            this.field2 = field2;
        }

        @GraphQLInputField
        public @GraphQLNonNull List<RelayTest.Book> getField3() {
            return field3;
        }

        // Only Gson will pick this up. In Jackson, the constructor parameter shadows the unannotated setter.
        public void setField3(List<RelayTest.@GraphQLScalar Book> field3) {
            this.field3 = field3;
        }
    }
}
