package io.leangen.graphql.util.classpath;

import java.io.IOException;

/**
 * Created by bojan.tomic on 6/18/16.
 */
public class ClassReadingException extends IOException {

	public ClassReadingException(String message, Throwable cause) {
		super(message, cause);
	}
}
