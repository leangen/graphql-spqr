package io.leangen.graphql.util.classpath;

/**
 * Holds information about a method within a class.
 *
 * @see ClassInfo
 */
public class MethodInfo implements Comparable<MethodInfo> {
    /*----------------------------------------------------------------------*\
                            Private Data Items
    \*----------------------------------------------------------------------*/

    private int access = 0;
    private String name = null;
    private String description = null;
    private String signature = null;
    private String[] exceptions = null;

    /*----------------------------------------------------------------------*\
                                Constructor
    \*----------------------------------------------------------------------*/

    /**
     * Create a new, empty <tt>MethodInfo</tt> object.
     */
    public MethodInfo() {
    }

    /**
     * Create and initialize a new <tt>MethodInfo</tt> object.
     *
     * @param access      method access modifiers, etc.
     * @param name        method name
     * @param description method description
     * @param signature   method signature
     * @param exceptions  list of thrown exceptions (by name)
     */
    public MethodInfo(int access,
                      String name,
                      String description,
                      String signature,
                      String[] exceptions) {
        this.access = access;
        this.name = name;
        this.description = description;
        this.signature = signature;
        this.exceptions = exceptions;
    }

    /*----------------------------------------------------------------------*\
                            Public Methods
    \*----------------------------------------------------------------------*/

    /**
     * Get the access modifiers for this method.
     *
     * @return the access modifiers, or 0 if none are set.
     */
    public int getAccess() {
        return access;
    }

    /**
     * Get the method name.
     *
     * @return the method name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the method description, if any.
     *
     * @return the method description, or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the method's signature, if any.
     *
     * @return the method signature, or null.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Get the class names of the thrown exceptions
     *
     * @return the names of the thrown exceptions, or null.
     */
    public String[] getExceptions() {
        return exceptions;
    }

    /**
     * Get the hash code. The hash code is based on the field's signature.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return signature.hashCode();
    }

    /**
     * Compare this object and another <tt>MethodInfo</tt> object. The two
     * objects are compared by their signature fields.
     *
     * @param other the other object
     * @return a negative integer, zero, or a positive integer, as this
     * object is less than, equal to, or greater than the specified
     * object.
     */
    public int compareTo(MethodInfo other) {
        return this.signature.compareTo(other.signature);
    }

    /**
     * Compare this object to another one for equality. If the other
     * object is a <tt>MethodInfo</tt> instance, the two will be compared by
     * signature.
     *
     * @param other the other object
     * @return <tt>true</tt> if <tt>other</tt> is a <tt>MethodInfo</tt>
     * object and it has the same signature as this object,
     * <tt>false</tt> otherwise.
     */
    public boolean equals(Object other) {
        return other instanceof MethodInfo && compareTo((MethodInfo) other) == 0;
    }

    /**
     * Return a string representation of the method. Currently, the string
     * representation is just the method's signature, or the name if the
     * signature is null.
     *
     * @return a string representation
     */
    public String toString() {
        return (signature != null) ? signature : name;
    }
}
