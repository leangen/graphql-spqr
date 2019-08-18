package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class PackageBasedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

    private final String packageName;

    public PackageBasedInterfaceStrategy(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public boolean supportsInterface(AnnotatedType inter) {
        return ClassUtils.isSubPackage(ClassUtils.getRawType(inter.getType()).getPackage(), packageName);
    }
}
