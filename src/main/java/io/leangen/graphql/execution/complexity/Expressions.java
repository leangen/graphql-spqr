package io.leangen.graphql.execution.complexity;

import java.util.*;

/**
 * Evaluates arithmetic expressions using Dijkstra's two-stack algorithm.
 * Handles the following binary operators: +, -, *, / and parentheses.
 * Source: <a href="https://algs4.cs.princeton.edu/13stacks/EvaluateDeluxe.java.html">EvaluateDeluxe</a>
 * Note: Operators must be left associative (exponentiation is right associative)
 */
public class Expressions {

    private static final Map<String, Integer> OPERATOR_PRECEDENCE;
    private static final String LOOK_AROUND_PATTERN = "((?<=%1$s)|(?=%1$s))";
    private static final String DELIMITER = String.format(LOOK_AROUND_PATTERN, "[\\Q()+-*/\\E]|\\s");

    static {
        TreeMap<String, Integer> precedence = new TreeMap<>();
        precedence.put("(", 0);   // for convenience with algorithm
        precedence.put(")", 0);
        precedence.put("+", 1);   // + and - have lower precedence than * and /
        precedence.put("-", 1);
        precedence.put("*", 2);
        precedence.put("/", 2);
        OPERATOR_PRECEDENCE = Collections.unmodifiableMap(precedence);
    }

    public static Double eval(String expression, Map<String, Object> variables) {
        Stack<String> ops  = new Stack<>();
        Stack<Double> vals = new Stack<>();

        Queue<String> tokens = new ArrayDeque<>(Arrays.asList(expression.split(DELIMITER)));
        while (!tokens.isEmpty()) {

            // read in the next token (operator or value)
            String s = tokens.poll();

            if (s.isBlank()) {
                continue;
            }

            // token is a value
            if (!OPERATOR_PRECEDENCE.containsKey(s)) {
                Object var = variables.get(s);
                if (var instanceof Number) {
                    vals.push(((Number) var).doubleValue());
                } else {
                    try {
                        vals.push(Double.parseDouble(var instanceof String ? var.toString() : s));
                    } catch (NumberFormatException e) {
                        if (var == null) {
                            throw new IllegalArgumentException("Undefined variable '" + s + "'");
                        }
                        throw e;
                    }
                }
                continue;
            }

            // token is an operator
            while (true) {

                // the last condition ensures that the operator with the higher precedence is evaluated first
                if (ops.isEmpty() || s.equals("(") || (OPERATOR_PRECEDENCE.get(s) > OPERATOR_PRECEDENCE.get(ops.peek()))) {
                    ops.push(s);
                    break;
                }

                // evaluate the expression
                String op = ops.pop();

                // but ignore left parentheses
                if (op.equals("(")) {
                    if (!s.equals(")")) {
                        throw new IllegalArgumentException("Unbalanced parentheses: ')' expected");
                    }
                    break;
                }

                // evaluate the operator and the two operands and push the result onto the value stack
                else {
                    double val2 = vals.pop();
                    double val1 = vals.pop();
                    vals.push(eval(op, val1, val2));
                }
            }
        }

        // finished parsing - evaluate the operator and the operands remaining on the two stacks
        while (!ops.isEmpty()) {
            String op = ops.pop();
            double val2 = vals.pop();
            double val1 = vals.pop();
            vals.push(eval(op, val1, val2));
        }

        // the last value on stack is the value of expression
        Double result = vals.pop();
        if (!vals.isEmpty()) {
            throw new IllegalArgumentException("Missing operator");
        }
        return result;
    }

    private static double eval(String op, double val1, double val2) {
        if (op.equals("+")) return val1 + val2;
        if (op.equals("-")) return val1 - val2;
        if (op.equals("/")) return val1 / val2;
        if (op.equals("*")) return val1 * val2;
        throw new IllegalArgumentException("Invalid operator '" + op + "'");
    }
}
