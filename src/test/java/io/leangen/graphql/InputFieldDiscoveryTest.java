package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.DefaultValue;
import io.leangen.graphql.annotations.Ignore;
import io.leangen.graphql.annotations.InputField;
import io.leangen.graphql.annotations.Query;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapper;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapper;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.junit.Test;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InputFieldDiscoveryTest {

    private JacksonValueMapper jackson = new JacksonValueMapperFactory().getValueMapper(Collections.emptyMap(), ENVIRONMENT);
    private GsonValueMapper gson = new GsonValueMapperFactory().getValueMapper(Collections.emptyMap(), ENVIRONMENT);

    private static final AnnotatedType IGNORED_TYPE = GenericTypeReflector.annotate(Object.class);
    private static final GlobalEnvironment ENVIRONMENT = new TestGlobalEnvironment();

    private static final io.leangen.graphql.metadata.InputField[] expectedDefaultFields = new io.leangen.graphql.metadata.InputField[] {
            new io.leangen.graphql.metadata.InputField("field1", null, IGNORED_TYPE, null, null, null),
            new io.leangen.graphql.metadata.InputField("field2", null, IGNORED_TYPE, null, null, null),
            new io.leangen.graphql.metadata.InputField("field3", null, IGNORED_TYPE, null, null, null)
    };
    private static final io.leangen.graphql.metadata.InputField[] expectedFilteredDefaultFields = new io.leangen.graphql.metadata.InputField[] {expectedDefaultFields[0], expectedDefaultFields[2]};
    private static final io.leangen.graphql.metadata.InputField[] expectedExplicitFields = new io.leangen.graphql.metadata.InputField[] {
            new io.leangen.graphql.metadata.InputField("aaa", "AAA", IGNORED_TYPE, null, "AAAA", null),
            new io.leangen.graphql.metadata.InputField("bbb", "BBB", IGNORED_TYPE, null, 2222, null),
            new io.leangen.graphql.metadata.InputField("ccc", "CCC", IGNORED_TYPE, null, 3333, null)
    };
    private static final io.leangen.graphql.metadata.InputField[] expectedQueryFields = new io.leangen.graphql.metadata.InputField[] {
            new io.leangen.graphql.metadata.InputField("aaa", null, IGNORED_TYPE, null, null, null),
            new io.leangen.graphql.metadata.InputField("bbb", null, IGNORED_TYPE, null, null, null),
            new io.leangen.graphql.metadata.InputField("ccc", null, IGNORED_TYPE, null, null, null)
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

    private void assertFieldNamesEqual(Class typeToScan, io.leangen.graphql.metadata.InputField... expectedFields) {
        Set<io.leangen.graphql.metadata.InputField> jFields = assertFieldNamesEqual(jackson, typeToScan, expectedFields);
        Set<io.leangen.graphql.metadata.InputField> gFields = assertFieldNamesEqual(gson, typeToScan, expectedFields);

        assertAllFieldsEqual(jFields, gFields);
    }

    private Set<io.leangen.graphql.metadata.InputField> assertFieldNamesEqual(InputFieldBuilder mapper, Class typeToScan, io.leangen.graphql.metadata.InputField[] templates) {
        Set<io.leangen.graphql.metadata.InputField> fields = mapper.getInputFields(
                InputFieldBuilderParams.builder()
                        .withType(GenericTypeReflector.annotate(typeToScan))
                        .withEnvironment(ENVIRONMENT)
                        .build());
        assertEquals(templates.length, fields.size());
        for (io.leangen.graphql.metadata.InputField template : templates) {
            Optional<io.leangen.graphql.metadata.InputField> field = fields.stream().filter(input -> input.getName().equals(template.getName())).findFirst();
            assertTrue("Field '" + template.getName() + "' doesn't match between different strategies", field.isPresent());
            assertEquals(template.getDescription(), field.get().getDescription());
            Object defaultValue = field.get().getDefaultValue();
            if (defaultValue instanceof Double) {
                defaultValue = ((Double) defaultValue).intValue();
            }
            assertEquals(template.getDefaultValue(), defaultValue);
        }
        return fields;
    }

    private void assertAllFieldsEqual(Set<io.leangen.graphql.metadata.InputField> fields1, Set<io.leangen.graphql.metadata.InputField> fields2) {
        assertEquals(fields1.size(), fields2.size());
        fields1.forEach(f1 -> assertTrue(fields2.stream().anyMatch(f2 -> f1.getName().equals(f2.getName())
                        && Objects.equals(f1.getDescription(), f2.getDescription())
                        && GenericTypeReflector.equals(f1.getJavaType(), f2.getJavaType())
                        && Objects.equals(f1.getDefaultValue(), forceToInt(f2.getDefaultValue())))));
    }

    private static Object forceToInt(Object val) {
        if (val instanceof Double) {
            return  ((Double) val).intValue();
        }
        return val;
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
        @InputField(name = "aaa", description = "AAA") @DefaultValue("AAAA")
        public String field1;
        @InputField(name = "bbb", description = "BBB") @DefaultValue("2222")
        public int field2;
        @InputField(name = "ccc", description = "CCC") @DefaultValue("3333")
        public Object field3;
    }

    private class ExplicitGetters {
        private String field1;
        private int field2;
        private Object field3;

        @InputField(name = "aaa", description = "AAA") @DefaultValue("AAAA")
        public String getField1() {
            return field1;
        }

        @InputField(name = "bbb", description = "BBB") @DefaultValue("2222")
        public int getField2() {
            return field2;
        }

        @InputField(name = "ccc", description = "CCC") @DefaultValue("3333")
        public Object getField3() {
            return field3;
        }
    }

    private class ExplicitSetters {
        private String field1;
        private int field2;
        private Object field3;

        @InputField(name = "aaa", description = "AAA") @DefaultValue("AAAA")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @InputField(name = "bbb", description = "BBB") @DefaultValue("2222")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @InputField(name = "ccc", description = "CCC") @DefaultValue("3333")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }
    
    private class QueryFields {
        @Query(value = "aaa")
        public String field1;
        @Query(value = "bbb")
        public int field2;
        @Query(value = "ccc")
        public Object field3;
    }

    private class QueryGetters {
        private String field1;
        private int field2;
        private Object field3;

        @Query(value = "aaa")
        public String getField1() {
            return field1;
        }

        @Query(value = "bbb")
        public int getField2() {
            return field2;
        }

        @Query(value = "ccc")
        public Object getField3() {
            return field3;
        }
    }

    private class QuerySetters {
        private String field1;
        private int field2;
        private Object field3;

        @Query(value = "aaa")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @Query(value = "bbb")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @Query(value = "ccc")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class MixedFieldsWin {
        @InputField(name = "aaa", description = "AAA") @DefaultValue("AAAA")
        private String field1;
        @InputField(name = "bbb", description = "BBB") @DefaultValue("2222")
        private int field2;
        @InputField(name = "ccc", description = "CCC") @DefaultValue("3333")
        private Object field3;

        @Query(value = "xxx")
        public String getField1() {
            return field1;
        }

        @Query(value = "yyy")
        public int getField2() {
            return field2;
        }

        @Query(value = "zzz")
        public Object getField3() {
            return field3;
        }
    }
    
    private class MixedGettersWin {
        @Query(value = "xxx")
        private String field1;
        @Query(value = "yyy")
        private int field2;
        @Query(value = "zzz")
        private Object field3;

        @InputField(name = "aaa", description = "AAA") @DefaultValue("AAAA")
        public String getField1() {
            return field1;
        }

        @InputField(name = "bbb", description = "BBB") @DefaultValue("2222")
        public int getField2() {
            return field2;
        }

        @InputField(name = "ccc", description = "CCC") @DefaultValue("3333")
        public Object getField3() {
            return field3;
        }
    }

    private class MixedSettersWin {
        @Query(value = "xxx")
        private String field1;
        @Query(value = "yyy")
        private int field2;
        @Query(value = "zzz")
        private Object field3;

        @InputField(name = "aaa", description = "AAA") @DefaultValue("AAAA")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @InputField(name = "bbb", description = "BBB") @DefaultValue("2222")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @InputField(name = "ccc", description = "CCC") @DefaultValue("3333")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class ConflictingGettersWin {
        @InputField(name = "xxx", description = "XXX") @DefaultValue("XXXX")
        private String field1;
        @InputField(name = "yyy", description = "YYY") @DefaultValue("-1")
        private int field2;
        @InputField(name = "zzz", description = "ZZZ") @DefaultValue("-1")
        private Object field3;

        @InputField(name = "aaa", description = "AAA") @DefaultValue("AAAA")
        public String getField1() {
            return field1;
        }

        @InputField(name = "bbb", description = "BBB") @DefaultValue("2222")
        public int getField2() {
            return field2;
        }

        @InputField(name = "ccc", description = "CCC") @DefaultValue("3333")
        public Object getField3() {
            return field3;
        }
    }
    
    private class ConflictingSettersWin {
        @InputField(name = "xxx", description = "XXX") @DefaultValue("XXXX")
        private String field1;
        @InputField(name = "yyy", description = "YYY") @DefaultValue("-1")
        private int field2;
        @InputField(name = "zzz", description = "ZZZ") @DefaultValue("-1")
        private Object field3;

        @InputField(name = "aaa", description = "AAA") @DefaultValue("AAAA")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @InputField(name = "bbb", description = "BBB") @DefaultValue("2222")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @InputField(name = "ccc", description = "CCC") @DefaultValue("3333")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class AllConflictingSettersWin {
        @InputField(name = "xxx", description = "XXX") @DefaultValue("XXXX")
        private String field1;
        @InputField(name = "yyy", description = "YYY") @DefaultValue("-1")
        private int field2;
        @InputField(name = "zzz", description = "ZZZ") @DefaultValue("-1")
        private Object field3;

        @InputField(name = "111", description = "1111") @DefaultValue("XXXX")
        public String getField1() {
            return field1;
        }

        @InputField(name = "222", description = "2222") @DefaultValue("-1")
        public int getField2() {
            return field2;
        }

        @InputField(name = "333", description = "3333") @DefaultValue("-1")
        public Object getField3() {
            return field3;
        }

        @InputField(name = "aaa", description = "AAA") @DefaultValue("AAAA")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @InputField(name = "bbb", description = "BBB") @DefaultValue("2222")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @InputField(name = "ccc", description = "CCC") @DefaultValue("3333")
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

        @Query(value = "ignored")
        @InputField(name = "ignored")
        public int getField2() {
            return field2;
        }

        @Ignore
        @InputField(name = "ignored")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        public Object getField3() {
            return field3;
        }

        @InputField
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    public static class HiddenCtorParams {
        private String field1;
        private int field2;
        private Object field3;

        @JsonCreator
        public HiddenCtorParams(String field1, @Ignore int field2, Object field3) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
        }
    }
}
