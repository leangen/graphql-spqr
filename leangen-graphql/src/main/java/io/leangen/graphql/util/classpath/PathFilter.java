package io.leangen.graphql.util.classpath;

import java.nio.file.Path;

/**
 * Created by bojan.tomic on 6/19/16.
 */
@FunctionalInterface
public interface PathFilter {

	boolean accept(Path path);
}
