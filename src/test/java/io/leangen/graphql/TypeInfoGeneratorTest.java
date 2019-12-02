package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.metadata.messages.EmptyMessageBundle;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import org.junit.Test;

import java.lang.reflect.AnnotatedType;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class TypeInfoGeneratorTest {

    private final AnnotatedType innermost = GenericTypeReflector.toCanonical(new TypeToken<Outer.Inner<String[]>.Innermost<LocalDate[]>>(){}.getAnnotatedType());
    private final AnnotatedType innermost2 = GenericTypeReflector.toCanonical(new TypeToken<Outer2<Number[]>.Inner<String[]>.Innermost<LocalDate[]>>(){}.getAnnotatedType());

    @Test
    public void nestedTypesStaticFullTest() {
        TypeInfoGenerator infoGenerator = new DefaultTypeInfoGenerator().withHierarchicalNames();
        assertEquals("TypeInfoGeneratorTest_Outer_Inner_StringArray_Secret_LocalDateArray", infoGenerator.generateTypeName(innermost, new EmptyMessageBundle()));
    }

    @Test
    public void nestedTypesStaticFlattenedTest() {
        TypeInfoGenerator infoGenerator = new DefaultTypeInfoGenerator().withHierarchicalNames(true);
        assertEquals("Inner_StringArray_Secret_LocalDateArray", infoGenerator.generateTypeName(innermost, new EmptyMessageBundle()));
    }

    @Test
    public void nestedTypesInstanceTest() {
        TypeInfoGenerator infoGenerator = new DefaultTypeInfoGenerator().withHierarchicalNames();
        assertEquals("TypeInfoGeneratorTest_Outer_NumberArray_Inner_StringArray_Secret_LocalDateArray", infoGenerator.generateTypeName(innermost2, new EmptyMessageBundle()));
    }

    private static class Outer {
        static class Inner<T1> {
            @GraphQLType(name = "Secret")
            class Innermost<T2> {
            }
        }
    }

    @GraphQLType(name = "Outer")
    private class Outer2<T1> {
        class Inner<T2> {
            @GraphQLType(name = "Secret")
            class Innermost<T3> {
            }
        }
    }
}
