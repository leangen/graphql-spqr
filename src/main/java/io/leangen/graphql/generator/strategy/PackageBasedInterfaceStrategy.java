package io.leangen.graphql.generator.strategy;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class PackageBasedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

    private String packageName;

    public PackageBasedInterfaceStrategy(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public boolean supportsInterface(AnnotatedType interfase) {
        Package pack = ClassUtils.getRawType(interfase.getType()).getPackage();
        return pack != null && pack.getName().startsWith(packageName);
    }
}
