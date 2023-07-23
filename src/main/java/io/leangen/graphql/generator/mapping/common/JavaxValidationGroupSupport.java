package io.leangen.graphql.generator.mapping.common;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.reflect.AnnotatedType;
import java.util.Set;

import static io.leangen.graphql.generator.mapping.common.BeanValidationGroupSupport.isEnabled;

/**
 * @author Sven Barden
 */
public class JavaxValidationGroupSupport implements BeanValidationGroupSupport {

    private final Set<Class<?>> activeGroups;

    public JavaxValidationGroupSupport(Set<Class<?>> activeGroups) {
        this.activeGroups = activeGroups;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean supports(AnnotatedType type) {
        boolean valid = true;
        NotNull notNull = type.getAnnotation(NotNull.class);
        NotEmpty notEmpty = type.getAnnotation(NotEmpty.class);
        NotBlank notBlank = type.getAnnotation(NotBlank.class);
        if (notNull != null) {
            valid = isEnabled(notNull.groups(), activeGroups);
        }
        if (valid && notEmpty != null) {
            valid = isEnabled(notEmpty.groups(), activeGroups);
        }
        if (valid && notBlank != null) {
            valid = isEnabled(notBlank.groups(), activeGroups);
        }
        return valid;
    }
}
