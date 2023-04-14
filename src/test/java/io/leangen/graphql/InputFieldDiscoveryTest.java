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
import io.leangen.graphql.metadata.DefaultValue;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapper;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapper;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.junit.Test;

import java.beans.ConstructorProperties;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.leangen.graphql.metadata.DefaultValue.EMPTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InputFieldDiscoveryTest {

    private final JacksonValueMapper jackson = new JacksonValueMapperFactory().getValueMapper();
    private final GsonValueMapper gson = new GsonValueMapperFactory().getValueMapper();

    private static final TypedElement IGNORED_TYPE = new TypedElement(GenericTypeReflector.annotate(Object.class), (AnnotatedElement) null);
    private static final GlobalEnvironment ENVIRONMENT = GlobalEnvironment.EMPTY;

    private static final InputField[] expectedDefaultFields = new InputField[] {
            new InputField("field1", null, IGNORED_TYPE, null, EMPTY),
            new InputField("field2", null, IGNORED_TYPE, null, EMPTY),
            new InputField("field3", null, IGNORED_TYPE, null, EMPTY)
    };
    private static final InputField[] expectedFilteredDefaultFields = new InputField[] {expectedDefaultFields[0], expectedDefaultFields[2]};
    private static final InputField[] expectedExplicitFields = new InputField[] {
            new InputField("aaa", "AAA", IGNORED_TYPE, null, new DefaultValue("AAAA")),
            new InputField("bbb", "BBB", IGNORED_TYPE, null, new DefaultValue(2222)),
            new InputField("ccc", "CCC", IGNORED_TYPE, null, new DefaultValue(3333))
    };
    private static final InputField[] expectedQueryFields = new InputField[] {
            new InputField("aaa", null, IGNORED_TYPE, null, EMPTY),
            new InputField("bbb", null, IGNORED_TYPE, null, EMPTY),
            new InputField("ccc", null, IGNORED_TYPE, null, EMPTY)
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
    public void mergedTypesTest() {
        Set<InputField> jFields = getInputFields(jackson, MergedTypes.class);
        Set<InputField> gFields = getInputFields(gson, MergedTypes.class);

        assertTypesMerged(jFields);
        assertTypesMerged(gFields);

        assertAllFieldsEqual(jFields, gFields);
    }

    @Test
    public void abstractInputTest() {
        assertFieldNamesEqual(Abstract.class, expectedDefaultFields);
    }

    @Test
    public void jacksonDelegatedConstructorTest() {
        assertFieldNamesEqual(jackson, Delegator.class, expectedDefaultFields);
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
                        .withConcreteSubTypes(Arrays.asList(Concrete.class, Concrete2.class))
                        .build());
    }

    private void assertAllFieldsEqual(Set<InputField> fields1, Set<InputField> fields2) {
        assertEquals(fields1.size(), fields2.size());
        fields1.forEach(f1 -> assertTrue(fields2.stream().anyMatch(f2 -> f1.getName().equals(f2.getName())
                        && Objects.equals(f1.getDescription(), f2.getDescription())
                        && GenericTypeReflector.equals(f1.getJavaType(), f2.getJavaType())
                        && Objects.equals(f1.getDefaultValue(), f2.getDefaultValue()))));
    }

    private static class FieldsOnly {
        public String field1;
        public int field2;
        public Object field3;
    }

    private static class GettersOnly {
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

    private static class SettersOnly {
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

    private static class ExplicitFields {
        @GraphQLInputField(name = "aaa", description = "AAA", defaultValue = "AAAA")
        public String field1;
        @GraphQLInputField(name = "bbb", description = "BBB", defaultValue = "2222")
        public int field2;
        @GraphQLInputField(name = "ccc", description = "CCC", defaultValue = "3333")
        public Object field3;
    }

    private static class ExplicitGetters {
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

    private static class ExplicitSetters {
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
    
    private static class QueryFields {
        @GraphQLQuery(name = "aaa")
        public String field1;
        @GraphQLQuery(name = "bbb")
        public int field2;
        @GraphQLQuery(name = "ccc")
        public Object field3;
    }

    private static class QueryGetters {
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

    private static class QuerySetters {
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

    private static class MixedFieldsWin {
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
    
    private static class MixedGettersWin {
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

    private static class MixedSettersWin {
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

    private static class ConflictingGettersWin {
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
    
    @SuppressWarnings("FieldCanBeLocal")
    private static class ConflictingSettersWin {
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

    private static class AllConflictingSettersWin {
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

    @SuppressWarnings("unused")
    private static class HiddenSetters {
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
        private final String field1;
        private final int field2;
        private final Object field3;

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

    private interface Abstract {
        @GraphQLInputField(name = "field1")
        String getFieldX();
        int getField2();
        String getField3();
        String getField4();
    }

    private static class Concrete implements Abstract {

        private final String fieldX;
        private final int field2;

        @JsonCreator
        @ConstructorProperties({"fieldX", "field2"})
        public Concrete(String model, int price) {
            this.fieldX = model;
            this.field2 = price;
        }

        @Override
        @GraphQLInputField(name = "field1")
        public String getFieldX() {
            return fieldX;
        }

        @Override
        public int getField2() {
            return field2;
        }

        @Override
        public String getField3() {
            return null;
        }

        @Override
        public String getField4() {
            return null;
        }
    }

    private static class Concrete2 implements Abstract {

        private String field3;
        private String field5;

        @Override
        public String getFieldX() {
            return null;
        }

        @Override
        public int getField2() {
            return 0;
        }

        @Override
        public String getField3() {
            return field3;
        }

        public void setField3(String field3) {
            this.field3 = field3;
        }

        @Override
        public String getField4() {
            return null;
        }

        public void setField5(String field5) {
            this.field5 = field5;
        }
    }

    private static class Delegate {

        private final String field1;
        private final int field2;
        private final Object field3;

        @JsonCreator
        public Delegate(String field1, int field2, Object field3) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
        }

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

    private static class Delegator {

        private final String field4;
        private final String field5;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Delegator(Delegate delegate) {
            this.field4 = delegate.field1;
            this.field5 = delegate.field2 + delegate.field3.toString();
        }

        public String getField4() {
            return field4;
        }

        public String getField5() {
            return field5;
        }
    }
}
