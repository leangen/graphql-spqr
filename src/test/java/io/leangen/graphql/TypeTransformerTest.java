package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeTransformer;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import org.junit.Test;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TypeTransformerTest {
    
    private final TypeTransformer explosive = new DefaultTypeTransformer(false, false);
    private final TypeTransformer replacing = new DefaultTypeTransformer(true, true);
    
    @Test
    public void testRawReplacement() {
        Type expected = new TypeToken<List<Map<Object, Object>>>(){}.getType();
        AnnotatedType subject = new TypeToken<List<Map>>(){}.getAnnotatedType();
        assertEquals(expected, replacing.transform(subject).getType());
    }
    
    @Test(expected = TypeMappingException.class)
    public void testRawError() {
        explosive.transform(GenericTypeReflector.annotate(List.class));
    }
    
    @Test
    public void testUnboundedReplacement() {
        Type expected = new TypeToken<List<Map<String, Object>>>(){}.getType();
        AnnotatedType subject = new TypeToken<List<Map<String, ?>>>(){}.getAnnotatedType();
        assertEquals(expected, replacing.transform(subject).getType());
    }
    
    @Test(expected = TypeMappingException.class)
    public void testUnboundedError() {
        Type wildCardList = GenericTypeReflector.addWildcardParameters(List.class);
        explosive.transform(GenericTypeReflector.annotate(wildCardList));
    }

    @Test
    public void testLowerBoundErasure() {
        Type expected = new TypeToken<List<Map<String, Number>>>(){}.getType();
        AnnotatedType subject = new TypeToken<List<Map<String, ? extends Number>>>(){}.getAnnotatedType();
        assertEquals(expected, replacing.transform(subject).getType());
    }
    
    @Test
    public void testUpperBoundErasure() {
        Type expected = new TypeToken<List<Map<String, Number>>>(){}.getType();
        AnnotatedType subject = new TypeToken<List<Map<String, ? super Number>>>(){}.getAnnotatedType();
        assertEquals(expected, replacing.transform(subject).getType());
    }
    
    @Test
    public void testCustomRawReplacement() {
        Type expected = new TypeToken<List<Map<Long, Long>>>(){}.getType();
        AnnotatedType subject = new TypeToken<List<Map>>(){}.getAnnotatedType();
        AnnotatedType replacement = GenericTypeReflector.annotate(Long.class);
        TypeTransformer longReplace = new DefaultTypeTransformer(replacement, null);
        assertEquals(expected, longReplace.transform(subject).getType());
    }

    @Test
    public void testCustomUnboundedReplacement() {
        Type expected = new TypeToken<List<Map<String, Double>>>(){}.getType();
        AnnotatedType subject = new TypeToken<List<Map<String, ?>>>(){}.getAnnotatedType();
        AnnotatedType replacement = GenericTypeReflector.annotate(Double.class);
        TypeTransformer longReplace = new DefaultTypeTransformer(null, replacement);
        assertEquals(expected, longReplace.transform(subject).getType());
    }
}
