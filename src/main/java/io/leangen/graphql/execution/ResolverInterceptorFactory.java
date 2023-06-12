package io.leangen.graphql.execution;

import java.util.Collections;
import java.util.List;

/**
 * Interceptors wrap around resolver method invocation, output conversion and, finally, around each other.
 * In this sense, they are similar to Servlet filters.
 * <p>An <em>inner</em> resolver is invoked around the resolver method, while an <em>outer</em> resolver is invoked around output conversion.</p>
 * <p>
 *     <strong>Example 1.</strong> Given one inner and one outer interceptor, the order of invocations would be:
 *     <ol>
 *     <li><font color="teal">outer: enter</font></li>
 *     <li>conversion: enter</li>
 *     <li><font color="fuchsia">inner: enter</font></li>
 *     <li><em>&lt;resolver method invoked&gt;</em></li>
 *     <li><font color="fuchsia">inner: exit</font></li>
 *     <li>conversion: exit</li>
 *     <li><font color="teal">outer: exit</font></li>
 *     </ol>
 * </p>
 * <p>If multiple inner or outer resolvers are registered, they are wrapped around each other in the same order they are produced by the instances of this class.</p>
 * <p>
 *     <strong>Example 2.</strong> Given two inner and two outer interceptor, the order of invocations would be:
 *     <ol>
 *     <li><font color="teal">outer1: enter</font></li>
 *     <li><font color="purple">outer2: enter</font></li>
 *     <li>conversion: enter</li>
 *     <li><font color="lime">inner1: enter</font></li>
 *     <li><font color="fuchsia">inner2: enter</font></li>
 *     <li><em>&lt;resolver method invoked&gt;</em></li>
 *     <li><font color="fuchsia">inner2: exit</font></li>
 *     <li><font color="lime">inner1: exit</font></li>
 *     <li>conversion: exit</li>
 *     <li><font color="purple">outer2: exit</font></li>
 *     <li><font color="teal">outer1: exit</font></li>
 *     </ol>
 * </p>
 */
public interface ResolverInterceptorFactory {

    /**
     * Produces <em>inner</em> ResolverInterceptors, invoked around the resolver method.<p/>
     * <p>Calling {@link io.leangen.graphql.execution.ResolverInterceptor.Continuation#proceed(InvocationContext)} produces the <em>raw (unconverted) result</em>.</p>
     * @param params Factory parameters
     * @return a list of outer interceptors
     */
    List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params);

    /**
     * Produces <em>outer</em> ResolverInterceptors, invoked around result conversion.
     * <p>Calling {@link io.leangen.graphql.execution.ResolverInterceptor.Continuation#proceed(InvocationContext)} produces the <em>converted result</em>.</p>
     * @param params Factory parameters
     * @return a list of outer interceptors
     */
    default List<ResolverInterceptor> getOuterInterceptors(ResolverInterceptorFactoryParams params) {
        return Collections.emptyList();
    }
}
