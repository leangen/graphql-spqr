/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.graphql;

/**
 * A GraphQLException is used to pass error information back to the client.
 * Instances of GraphQLException and it's subclasses will pass the
 * <code>message</code> field back to the client. The optional
 * <code>ExceptionType</code> may also be specified.
 * 
 * While it is expected that MP GraphQL implementations will return the message
 * to the client, implementations must not return the stack trace not the cause
 * to the client.
 */
public class GraphQLException extends Exception {
    private static final long serialVersionUID = -3661091414653921754L;

    public static enum ExceptionType {
        DataFetchingException, OperationNotSupported, ExecutionAborted
    }

    private ExceptionType type;

    private Object partialResults;

    public GraphQLException() {
        super();
    }

    public GraphQLException(String message) {
        super(message);
    }

    public GraphQLException(Throwable cause) {
        super(cause);
    }

    public GraphQLException(String message, Throwable cause) {
        super(message, cause);
    }

    public GraphQLException(ExceptionType type) {
        super();
        this.type = type;
    }

    public GraphQLException(String message, ExceptionType type) {
        super(message);
        this.type = type;
    }

    public GraphQLException(Throwable cause, ExceptionType type) {
        super(cause);
        this.type = type;
    }

    public GraphQLException(String message, Throwable cause, ExceptionType type) {
        super(message, cause);
        this.type = type;
    }

    public GraphQLException(Object partialResults) {
        super();
        this.partialResults = partialResults;
    }

    public GraphQLException(String message, Object partialResults) {
        super(message);
        this.partialResults = partialResults;
    }

    public GraphQLException(Throwable cause, Object partialResults) {
        super(cause);
        this.partialResults = partialResults;
    }

    public GraphQLException(String message, Throwable cause, Object partialResults) {
        super(message, cause);
        this.partialResults = partialResults;
    }

    public java.lang.Object getPartialResults() {
        return partialResults;
    }

    public void setPartialResults(Object partialResults) {
        this.partialResults = partialResults;
    }

    public ExceptionType getExceptionType() {
        return type;
    }

    public void setExceptionType(ExceptionType type) {
        this.type = type;
    }
}