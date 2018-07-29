package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.Method;

public class PropertyOperationNameGenerator extends AnnotatedOperationNameGenerator {

    @Override
    public String generateQueryName(OperationNameGeneratorParams<?> params) {
        String name = super.generateQueryName(params);
        if (Utils.isEmpty(name)) {
            if (params.isField()) {
                return params.getElement().getName();
            }
            Method queryMethod = (Method) params.getElement();
            if (ClassUtils.isGetter(queryMethod)) {
                name = ClassUtils.getFieldNameFromGetter(queryMethod);
            }
        }
        return name;
    }

    @Override
    public String generateMutationName(OperationNameGeneratorParams<Method> params) {
        String name = super.generateMutationName(params);
        if (Utils.isEmpty(name) && ClassUtils.isSetter(params.getElement())) {
            name = ClassUtils.getFieldNameFromSetter(params.getElement());
        }
        return name;
    }
}
