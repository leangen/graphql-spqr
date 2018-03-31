package io.leangen.graphql.util.classpath;

import java.util.List;

public class AnnotationInfo {

    private final String className;
    private final List<Object> values;

    AnnotationInfo(String className, List<Object> values) {
        this.className = className;
        this.values = values;
    }

    public String getClassName() {
        return className;
    }

    public List<Object> getValues() {
        return values;
    }
}
