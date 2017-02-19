package io.leangen.graphql.util;

import java.util.ArrayList;
import java.util.List;

import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;
import io.leangen.graphql.query.relay.CursorProvider;
import io.leangen.graphql.query.relay.Edge;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.query.relay.generic.GenericEdge;
import io.leangen.graphql.query.relay.generic.GenericPage;
import io.leangen.graphql.query.relay.generic.GenericPageInfo;

public class GraphQLUtils {

    public static final String BASIC_INTROSPECTION_QUERY = "{ __schema { queryType { name fields { name type { name kind ofType { name kind fields { name } }}}}}}";

    public static final String FULL_INTROSPECTION_QUERY = "query IntrospectionQuery { __schema { queryType { name } mutationType { name } types { ...FullType } directives { name description args { ...InputValue } onOperation onFragment onField } } } fragment FullType on __Type { kind name description fields(includeDeprecated: true) { name description args { ...InputValue } type { ...TypeRef } isDeprecated deprecationReason } inputFields { ...InputValue } interfaces { ...TypeRef } enumValues(includeDeprecated: true) { name description isDeprecated deprecationReason } possibleTypes { ...TypeRef } } fragment InputValue on __InputValue { name description type { ...TypeRef } defaultValue } fragment TypeRef on __Type { kind name ofType { kind name ofType { kind name ofType { kind name } } } }";

    public static GraphQLType unwrapNonNull(GraphQLType type) {
        if (type instanceof GraphQLNonNull) {
            return unwrapNonNull(((GraphQLNonNull) type).getWrappedType());
        }
        return type;
    }
    
    public static <N> Page<N> createOffsetBasedPage(List<N> nodes, long count, long offset) {
        return createOffsetBasedPage(nodes, offset, offset + nodes.size() < count, offset > 0 && count > 0);
    }

    public static <N> Page<N> createOffsetBasedPage(List<N> nodes, long offset, boolean hasNextPage, boolean hasPreviousPage) {
        return createPage(nodes, (node, index) -> Long.toString(offset + index + 1), hasNextPage, hasPreviousPage);
    }

    public static <N> Page<N> createPage(List<N> nodes, CursorProvider<N> cursorProvider, boolean hasNextPage, boolean hasPreviousPage) {
        List<Edge<N>> edges = createEdges(nodes, cursorProvider);
        return new GenericPage<>(edges, createPageInfo(edges, hasNextPage, hasPreviousPage));
    }

    public static <N> List<Edge<N>> createEdges(List<N> nodes, CursorProvider<N> cursorProvider) {
        List<Edge<N>> edges = new ArrayList<>(nodes.size());
        int index = 0;
        for (N node : nodes) {
            edges.add(new GenericEdge<>(node, cursorProvider.createCursor(node, index++)));
        }
        return edges;
    }

    public static <N> GenericPageInfo<N> createPageInfo(List<Edge<N>> edges, boolean hasNextPage, boolean hasPreviousPage) {
        return new GenericPageInfo<>(edges.get(0).getCursor(), edges.get(edges.size() - 1).getCursor(), hasNextPage, hasPreviousPage);
    }
}
