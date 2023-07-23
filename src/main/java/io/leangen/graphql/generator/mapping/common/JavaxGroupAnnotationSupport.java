package io.leangen.graphql.generator.mapping.common;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.lang.reflect.AnnotatedType;

/**
 * @author Sven Barden
 */
public class JavaxGroupAnnotationSupport implements GroupAnnotationSupport {
    @Override
    public boolean support(AnnotatedType type) {
        boolean valid = true;
        NotNull notNull = type.getAnnotation(NotNull.class);
        NotEmpty notEmpty = type.getAnnotation(NotEmpty.class);
        NotBlank notBlank = type.getAnnotation(NotBlank.class);
        if (notNull != null) {
            valid = notNull.groups().length == 0;
        }
        if (valid && notEmpty != null) {
            valid = notEmpty.groups().length == 0;
        }
        if (valid && notBlank != null) {
            valid = notBlank.groups().length == 0;
        }
        return valid;
    }
}
