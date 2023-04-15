package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.strategy.value.Property;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Set;

public class RecordResolverBuilder extends PublicResolverBuilder {

    public RecordResolverBuilder(String... basePackages) {
        super(basePackages);
        this.operationInfoGenerator = new RecordComponentInfoGenerator();
    }

    @Override
    protected Set<Property> findProperties(ResolverBuilderParams params) {
        return ClassUtils.getRecordComponents(params.getBeanType());
    }

    @Override
    protected boolean isQuery(Property property, ResolverBuilderParams params) {
        return isPackageAcceptable(property.getGetter(), params);
    }

    @Override
    protected boolean isQuery(Method method, ResolverBuilderParams params) {
        return false;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isRecord(type);
    }
}
