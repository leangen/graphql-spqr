package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class BaseTypeAliasComparator implements Comparator<AnnotatedType> {

    private final Set<Type> aliasGroup;

    public BaseTypeAliasComparator(Type... aliasGroup) {
        this.aliasGroup = new HashSet<>();
        Collections.addAll(this.aliasGroup, aliasGroup);
    }

    @Override
    public int compare(AnnotatedType o1, AnnotatedType o2) {
        if (aliasGroup.contains(o1.getType()) && aliasGroup.contains(o2.getType())
                && Arrays.stream(o1.getAnnotations()).allMatch(ann -> Arrays.asList(o2.getAnnotations()).contains(ann))) {
            return 0;
        }
        return -1;
    }
}
