package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.strategy.DefaultInclusionStrategy;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.value.InputFieldDiscoveryStrategy;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapper;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapper;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InputFieldDiscoveryTest {

    private JacksonValueMapper jackson = new JacksonValueMapperFactory().getValueMapper();
    private GsonValueMapper gson = new GsonValueMapperFactory().getValueMapper();

    private InclusionStrategy inclusionStrategy = new DefaultInclusionStrategy("io.leangen");

    private static final String[] expectedDefaultFieldNames = {"field1", "field2", "field3"};
    private static final String[] expectedExplicitFieldNames = {"aaa", "bbb", "ccc"};
    
    @Test
    public void basicFieldsTest() {
        assertFieldNamesEqual(FieldsOnly.class, expectedDefaultFieldNames);
    }

    @Test
    public void basicGettersTest() {
        assertFieldNamesEqual(GettersOnly.class, expectedDefaultFieldNames);
    }

    @Test
    public void basicSettersTest() {
        assertFieldNamesEqual(SettersOnly.class, expectedDefaultFieldNames);
    }

    @Test
    public void explicitFieldsTest() {
        assertFieldNamesEqual(ExplicitFields.class, expectedExplicitFieldNames);
    }

    @Test
    public void explicitGettersTest() {
        assertFieldNamesEqual(ExplicitGetters.class, expectedExplicitFieldNames);
    }

    @Test
    public void explicitSettersTest() {
        assertFieldNamesEqual(ExplicitSetters.class, expectedExplicitFieldNames);
    }
    
    @Test
    public void queryFieldsTest() {
        assertFieldNamesEqual(QueryFields.class, expectedExplicitFieldNames);
    }

    @Test
    public void queryGettersTest() {
        assertFieldNamesEqual(QueryGetters.class, expectedExplicitFieldNames);
    }

    @Test
    public void querySettersTest() {
        assertFieldNamesEqual(QuerySetters.class, expectedExplicitFieldNames);
    }

    @Test
    public void mixedFieldsTest() {
        assertFieldNamesEqual(MixedFieldsWin.class, expectedExplicitFieldNames);
    }
    
    @Test
    public void mixedGettersTest() {
        assertFieldNamesEqual(MixedGettersWin.class, expectedExplicitFieldNames);
    }

    @Test
    public void mixedSettersTest() {
        assertFieldNamesEqual(MixedSettersWin.class, expectedExplicitFieldNames);
    }

    @Test
    public void conflictingGettersTest() {
        assertFieldNamesEqual(ConflictingGettersWin.class, expectedExplicitFieldNames);
    }

    @Test
    public void conflictingSettersTest() {
        assertFieldNamesEqual(ConflictingSettersWin.class, expectedExplicitFieldNames);
    }

    @Test
    public void allConflictingSettersTest() {
        assertFieldNamesEqual(AllConflictingSettersWin.class, expectedExplicitFieldNames);
    }

    @Test
    public void hiddenSettersTest() {
        assertFieldNamesEqual(HiddenSetters.class, "field1", "field3");
    }

    @Test
    public void hiddenCtorParamsTest() {
        assertFieldNamesEqual(jackson, HiddenCtorParams.class, "field1", "field3");
    }

    private void assertFieldNamesEqual(Class typeToScan, String... fieldNames) {
        Set<InputField> jFields = assertFieldNamesEqual(jackson, typeToScan, fieldNames);
        Set<InputField> gFields = assertFieldNamesEqual(gson, typeToScan, fieldNames);

        assertEquals(jFields, gFields);
    }

    private Set<InputField> assertFieldNamesEqual(InputFieldDiscoveryStrategy mapper, Class typeToScan, String... fieldNames) {
        Set<InputField> fields = mapper.getInputFields(GenericTypeReflector.annotate(typeToScan), inclusionStrategy);
        assertEquals(fieldNames.length, fields.size());
        for (String fieldName : fieldNames) {
            assertTrue(fields.stream().anyMatch(input -> input.getName().equals(fieldName)));
        }
        return fields;
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
        @GraphQLInputField(name = "aaa")
        public String field1;
        @GraphQLInputField(name = "bbb")
        public int field2;
        @GraphQLInputField(name = "ccc")
        public Object field3;
    }

    private class ExplicitGetters {
        private String field1;
        private int field2;
        private Object field3;

        @GraphQLInputField(name = "aaa")
        public String getField1() {
            return field1;
        }

        @GraphQLInputField(name = "bbb")
        public int getField2() {
            return field2;
        }

        @GraphQLInputField(name = "ccc")
        public Object getField3() {
            return field3;
        }
    }

    private class ExplicitSetters {
        private String field1;
        private int field2;
        private Object field3;

        @GraphQLInputField(name = "aaa")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLInputField(name = "bbb")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @GraphQLInputField(name = "ccc")
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
        @GraphQLInputField(name = "aaa")
        private String field1;
        @GraphQLInputField(name = "bbb")
        private int field2;
        @GraphQLInputField(name = "ccc")
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

        @GraphQLInputField(name = "aaa")
        public String getField1() {
            return field1;
        }

        @GraphQLInputField(name = "bbb")
        public int getField2() {
            return field2;
        }

        @GraphQLInputField(name = "ccc")
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

        @GraphQLInputField(name = "aaa")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLInputField(name = "bbb")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @GraphQLInputField(name = "ccc")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class ConflictingGettersWin {
        @GraphQLInputField(name = "xxx")
        private String field1;
        @GraphQLInputField(name = "yyy")
        private int field2;
        @GraphQLInputField(name = "zzz")
        private Object field3;

        @GraphQLInputField(name = "aaa")
        public String getField1() {
            return field1;
        }

        @GraphQLInputField(name = "bbb")
        public int getField2() {
            return field2;
        }

        @GraphQLInputField(name = "ccc")
        public Object getField3() {
            return field3;
        }
    }
    
    private class ConflictingSettersWin {
        @GraphQLInputField(name = "xxx")
        private String field1;
        @GraphQLInputField(name = "yyy")
        private int field2;
        @GraphQLInputField(name = "zzz")
        private Object field3;

        @GraphQLInputField(name = "aaa")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLInputField(name = "bbb")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @GraphQLInputField(name = "ccc")
        public void setField3(Object field3) {
            this.field3 = field3;
        }
    }

    private class AllConflictingSettersWin {
        @GraphQLInputField(name = "xxx")
        private String field1;
        @GraphQLInputField(name = "yyy")
        private int field2;
        @GraphQLInputField(name = "zzz")
        private Object field3;

        @GraphQLInputField(name = "111")
        public String getField1() {
            return field1;
        }

        @GraphQLInputField(name = "222")
        public int getField2() {
            return field2;
        }

        @GraphQLInputField(name = "333")
        public Object getField3() {
            return field3;
        }

        @GraphQLInputField(name = "aaa")
        public void setField1(String field1) {
            this.field1 = field1;
        }

        @GraphQLInputField(name = "bbb")
        public void setField2(int field2) {
            this.field2 = field2;
        }

        @GraphQLInputField(name = "ccc")
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
}
