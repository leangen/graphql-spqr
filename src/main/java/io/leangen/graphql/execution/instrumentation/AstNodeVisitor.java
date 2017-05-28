package io.leangen.graphql.execution.instrumentation;

import graphql.execution.instrumentation.parameters.DataFetchParameters;
import graphql.execution.instrumentation.parameters.FieldFetchParameters;

public interface AstNodeVisitor {
    
    void onNode(QueryTreeNode node);
    
    void onTreeComplete(QueryTreeNode root, DataFetchParameters parameters);
    
    void beginFiledFetch(FieldFetchParameters parameters);
}
