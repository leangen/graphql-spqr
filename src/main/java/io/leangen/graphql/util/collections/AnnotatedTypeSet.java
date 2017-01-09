package io.leangen.graphql.util.collections;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class AnnotatedTypeSet implements Set<AnnotatedType> {
    
    private final Set<AnnotatedType> knownTypes = new HashSet<>();

    @Override
    public int size() {
        return this.knownTypes.size();
    }

    @Override
    public boolean isEmpty() {
        return this.knownTypes.isEmpty();
    }

    @Override
    public boolean contains(Object type) {
        return type instanceof AnnotatedType && this.knownTypes.contains(wrap((AnnotatedType) type));
    }

    @Override
    public Iterator<AnnotatedType> iterator() {
        return this.knownTypes.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.knownTypes.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this.knownTypes.toArray(a);
    }

    @Override
    public boolean add(AnnotatedType type) {
        return this.knownTypes.add(wrap(type));
    }

    @Override
    public boolean remove(Object o) {
        return o instanceof AnnotatedType && this.knownTypes.remove(wrap((AnnotatedType) o));
    }

    @Override
    public boolean containsAll(Collection<?> types) {
        return types.stream().allMatch(element -> element instanceof AnnotatedType)
                && this.knownTypes.containsAll(wrapValid(types));
    }

    @Override
    public boolean addAll(Collection<? extends AnnotatedType> types) {
        return this.knownTypes.addAll(wrapValid(types));
    }

    @Override
    public boolean retainAll(Collection<?> types) {
        return this.knownTypes.retainAll(wrapValid(types));
    }

    @Override
    public boolean removeAll(Collection<?> types) {
        return this.knownTypes.removeAll(wrapValid(types));
    }

    @Override
    public void clear() {
        this.knownTypes.clear();
    }

    private AnnotatedType wrap(AnnotatedType type) {
        if (type.getClass().getPackage().equals(GenericTypeReflector.class.getPackage())) {
            return type;
        }
        return GenericTypeReflector.clone(type);
    }
    
    @SuppressWarnings("unchecked")
    private Collection<AnnotatedType> wrapValid(Collection<?> types) {
        if (types instanceof AnnotatedTypeSet) return (AnnotatedTypeSet) types;
        return types.stream()
                .filter(type -> type instanceof AnnotatedType)
                .map(type -> wrap((AnnotatedType) type))
                .collect(Collectors.toList());
    }
}
