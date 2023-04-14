package io.leangen.graphql.util;

import graphql.ExecutionInput;
import graphql.GraphQLContext;

import static io.leangen.graphql.util.GraphQLUtils.CLIENT_MUTATION_ID;

public class ContextUtils {

    public static ExecutionInput wrapContext(ExecutionInput executionInput) {
        return isDefault(executionInput.getContext())
                ? executionInput
                : executionInput.transform(input -> input.context(ctx ->
                ctx.of(ContextKey.class, executionInput.getContext())));
    }

    @SuppressWarnings("unchecked")
    public static <T> T unwrapContext(Object context) {
        if (isDefault(context)) {
            GraphQLContext ctx = (GraphQLContext) context;
            if (ctx.hasKey(ContextKey.class)) {
                return ctx.get(ContextKey.class);
            }
        }
        return (T) context;
    }

    public static boolean isDefault(Object context) {
        return context != null && GraphQLContext.class.equals(context.getClass());
    }

    public static String getClientMutationId(Object context) {
        if (isDefault(context)) {
            return ((GraphQLContext) context).get(CLIENT_MUTATION_ID);
        }
        return null;
    }

    public static void setClientMutationId(Object context, String clientMutationId) {
        if (Utils.isNotEmpty(clientMutationId)) {
            if (isDefault(context)) {
                ((GraphQLContext) context).put(CLIENT_MUTATION_ID, clientMutationId);
            }
        }
    }

    private static class ContextKey {}
}
