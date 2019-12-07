package io.leangen.graphql.generator.mapping;

import io.leangen.geantyref.AnnotatedTypeSet;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

public class SynonymTypeComparator implements Comparator<AnnotatedType> {

    private final Set<AnnotatedType> synonymGroup;

    public SynonymTypeComparator(AnnotatedType... synonymGroup) {
        Set<AnnotatedType> synonyms = new AnnotatedTypeSet<>();
        Collections.addAll(synonyms, synonymGroup);
        this.synonymGroup = Collections.unmodifiableSet(synonyms);
    }

    @Override
    public int compare(AnnotatedType t1, AnnotatedType t2) {
        return synonymGroup.contains(t1) && synonymGroup.contains(t2) ? 0 : -1;
    }
}
