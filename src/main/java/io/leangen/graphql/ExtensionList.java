package io.leangen.graphql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@SuppressWarnings("WeakerAccess")
public class ExtensionList<E> extends ArrayList<E> {

    public ExtensionList(Collection<? extends E> c) {
        super(c);
    }

    @SuppressWarnings("unchecked")
    public <T extends E> T getFirstOfType(Class<T> extensionType) {
        return (T) get(firstIndexOfTypeStrict(extensionType));
    }

    @SafeVarargs
    public final ExtensionList<E> append(E... extensions) {
        Collections.addAll(this, extensions);
        return this;
    }

    public ExtensionList<E> append(Collection<E> extensions) {
        super.addAll(extensions);
        return this;
    }

    @SafeVarargs
    public final ExtensionList<E> prepend(E... extensions) {
        return insert(0, extensions);
    }

    @SafeVarargs
    public final ExtensionList<E> insert(int index, E... extensions) {
        for (int i = 0; i < extensions.length; i++) {
            add(index + i, extensions[i]);
        }
        return this;
    }

    @SafeVarargs
    public final ExtensionList<E> insertAfter(Class<? extends E> extensionType, E... extensions) {
        return insert(firstIndexOfTypeStrict(extensionType) + 1, extensions);
    }

    @SafeVarargs
    public final ExtensionList<E> insertAfter(E extension, E... extensions) {
        return insert(indexOf(extension) + 1, extensions);
    }

    @SafeVarargs
    public final ExtensionList<E> insertBefore(Class<? extends E> extensionType, E... extensions) {
        return insert(firstIndexOfTypeStrict(extensionType), extensions);
    }

    @SafeVarargs
    public final ExtensionList<E> insertBefore(E extension, E... extensions) {
        return insert(indexOf(extension), extensions);
    }

    @SafeVarargs
    public final ExtensionList<E> insertAfterOrAppend(Class<? extends E> extensionType, E... extensions) {
        int firstIndexOfType = firstIndexOfType(extensionType);
        if (firstIndexOfType >= 0) {
            return insert(firstIndexOfType + 1, extensions);
        } else {
            return append(extensions);
        }
    }

    @SafeVarargs
    public final ExtensionList<E> insertBeforeOrPrepend(Class<? extends E> extensionType, E... extensions) {
        int firstIndexOfType = firstIndexOfType(extensionType);
        return insert(Math.max(firstIndexOfType, 0), extensions);
    }

    public ExtensionList<E> drop(int index) {
        super.remove(index);
        return this;
    }

    public ExtensionList<E> drop(Class<? extends E> extensionType) {
        return drop(firstIndexOfTypeStrict(extensionType));
    }

    public ExtensionList<E> dropAll(Predicate<? super E> filter) {
        super.removeIf(filter);
        return this;
    }

    public ExtensionList<E> replace(int index, UnaryOperator<E> replacer) {
        super.set(index, replacer.apply(get(index)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends E> ExtensionList<E> replace(Class<T> extensionType, Function<T, E> replacer) {
        int index = firstIndexOfTypeStrict(extensionType);
        super.set(index, replacer.apply((T) get(index)));
        return this;
    }

    public ExtensionList<E> replaceOrAppend(Class<? extends E> extensionType, E replacement) {
        int firstIndexOfType = firstIndexOfType(extensionType);
        if (firstIndexOfType >= 0) {
            return replace(firstIndexOfType, e -> replacement);
        } else {
            return append(replacement);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends E> ExtensionList<E> modify(Class<T> extensionType, Consumer<T> modifier) {
        modifier.accept((T) get(firstIndexOfTypeStrict(extensionType)));
        return this;
    }

    public ExtensionList<E> modifyAll(Predicate<? super E> test, Consumer<E> modifier) {
        super.stream().filter(test).forEach(modifier);
        return this;
    }

    public ExtensionList<E> modifyAll(Consumer<E> modifier) {
        return modifyAll(element -> true, modifier);
    }

    private int firstIndexOfTypeStrict(Class<? extends E> extensionType) {
        int firstIndexOfType = firstIndexOfType(extensionType);
        if (firstIndexOfType < 0) {
            throw new ConfigurationException("Extension of type " + extensionType.getName() + " not found");
        }
        return firstIndexOfType;
    }

    private int firstIndexOfType(Class<? extends E> extensionType) {
        for (int i = 0; i < size(); i++) {
            if (extensionType.equals(get(i).getClass())) {
                return i;
            }
        }
        return -1;
    }
}
