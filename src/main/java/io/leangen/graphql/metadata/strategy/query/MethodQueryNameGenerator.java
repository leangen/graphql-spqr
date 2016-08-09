package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public class MethodQueryNameGenerator implements QueryNameGenerator {

	@Override
	public String generateQueryName(Method queryMethod, AnnotatedType type) {
		return queryMethod.getName();
	}

	@Override
	public String generateQueryName(Field domainField, AnnotatedType declaringType) {
		return domainField.getName();
	}

	@Override
	public String generateMutationName(Method mutationMethod, AnnotatedType declaringType) {
		return mutationMethod.getName();
	}
}
