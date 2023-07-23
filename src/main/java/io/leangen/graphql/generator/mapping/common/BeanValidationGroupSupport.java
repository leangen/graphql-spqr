package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Set;

/**
 * @author Sven Barden
 */
public interface BeanValidationGroupSupport {

    boolean supports(AnnotatedType type);

    static boolean isEnabled(Class<?>[] groups, Set<Class<?>> activeGroups) {
        return groups.length == 0 //optimization
                || Arrays.stream(groups).anyMatch(activeGroups::contains);
    }

    interface GraphQL {}
}
