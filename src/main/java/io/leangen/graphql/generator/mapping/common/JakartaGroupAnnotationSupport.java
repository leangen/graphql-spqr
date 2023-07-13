package io.leangen.graphql.generator.mapping.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.lang.reflect.AnnotatedType;

/**
 * @author Sven Barden
 */
class JakartaGroupAnnotationSupport implements GroupAnnotationSupport {

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
