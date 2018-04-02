/*---------------------------------------------------------------------------*\
  $Id$
  ---------------------------------------------------------------------------
  This software is released under a BSD-style license:

  Copyright (c) 2004-2007 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  1.  Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

  2.  The end-user documentation included with the redistribution, if any,
      must include the following acknowlegement:

        "This product includes software developed by Brian M. Clapper
        (bmc@clapper.org, http://www.clapper.org/bmc/). That software is
        copyright (c) 2004-2007 Brian M. Clapper."

      Alternately, this acknowlegement may appear in the software itself,
      if wherever such third-party acknowlegements normally appear.

  3.  Neither the names "clapper.org", "clapper.org Java Utility Library",
      nor any of the names of the project contributors may be used to
      endorse or promote products derived from this software without prior
      written permission. For written permission, please contact
      bmc@clapper.org.

  4.  Products derived from this software may not be called "clapper.org
      Java Utility Library", nor may "clapper.org" appear in their names
      without prior written permission of Brian M. Clapper.

  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
  NO EVENT SHALL BRIAN M. CLAPPER BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
\*---------------------------------------------------------------------------*/

package io.leangen.graphql.util.classpath;

import java.util.Arrays;

/**
 * Instances of classes that implement this interface are used, with a
 * {@link ClassFinder} object, to filter class names. This interface is
 * deliberately reminiscent of the <tt>java.io.FilenameFilter</tt>
 * interface.
 *
 * @author Copyright &copy; 2006 Brian M. Clapper
 * @version <tt>$Revision$</tt>
 * @see ClassFinder
 */
@FunctionalInterface
public interface ClassFilter {

    /**
     * Tests whether a class name should be included in a class name
     * list.
     *
     * @param classInfo   the loaded information about the class
     * @param classFinder the {@link ClassFinder} that called this filter
     *                    (mostly for access to <tt>ClassFinder</tt>
     *                    utility methods)
     * @return <tt>true</tt> if and only if the name should be included
     * in the list; <tt>false</tt> otherwise
     */
    boolean accept(ClassInfo classInfo, ClassFinder classFinder);

    /**
     * Produces a {@code ClassFilter} that accepts an entry if all the delegate filters accept it.
     *
     * @param delegateFilters The delegate filters to propagate the call to
     * @return The filter combining the {@code delegateFilters} using a logical <i>and</i>
     */
    static ClassFilter and(ClassFilter... delegateFilters) {
        return (classInfo, classFinder) -> Arrays.stream(delegateFilters)
                .map(filter -> filter.accept(classInfo, classFinder))
                .reduce((f1, f2) -> f1 && f2)
                .orElse(true);
    }
}
