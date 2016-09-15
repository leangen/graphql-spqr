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

import java.io.File;

/**
 * Miscellaneous class-related utility methods.
 *
 * @author Copyright &copy; 2006 Brian M. Clapper
 * @version <tt>$Revision$</tt>
 */
public class ClassUtil {
    /*----------------------------------------------------------------------*\
                         Package-visible Constants
    \*----------------------------------------------------------------------*/

    static final String BUNDLE_NAME = "org.clapper.util.classutil.Bundle";

    /*----------------------------------------------------------------------*\
                                Static Data
    \*----------------------------------------------------------------------*/

    /*----------------------------------------------------------------------*\
                               Constructor
    \*----------------------------------------------------------------------*/

    /**
     * Can't be instantiated
     */
    private ClassUtil() {
        // Can't be instantiated
    }

    /*----------------------------------------------------------------------*\
                              Public Methods
    \*----------------------------------------------------------------------*/

    /**
     * Determine whether a file is a jar file, zip file or directory (i.e.,
     * represents places that can be searched for classes).
     *
     * @param file the file to check
     * @return <tt>true</tt> if the file represents a place that can be
     * searched for classes, <tt>false</tt> if not
     */
    public static boolean fileCanContainClasses(File file) {
        boolean can = false;
        String fileName = file.getPath();

        if (file.exists()) {
            can = ((fileName.toLowerCase().endsWith(".jar")) ||
                    (fileName.toLowerCase().endsWith(".zip")) ||
                    (file.isDirectory()));
        }

        return can;
    }

    /**
     * Strip the package name from a fully-qualified class name and return
     * just the short class name.
     *
     * @param fullClassName the full class name
     * @return the short name
     */
    public static String getShortClassName(String fullClassName) {
        String shortClassName = fullClassName;
        int i = shortClassName.lastIndexOf('.');

        if ((i != -1) && (++i < shortClassName.length()))
            shortClassName = shortClassName.substring(i);

        return shortClassName;
    }

    /**
     * Strip the package name from a fully-qualified class name and return
     * just the short class name.
     *
     * @param cls the <tt>Class</tt> object whose name is to be trimmed
     * @return the short name
     */
    public static String getShortClassName(Class cls) {
        return getShortClassName(cls.getName());
    }
}
