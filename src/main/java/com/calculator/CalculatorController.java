package com.calculator;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedList;
import java.util.Stack;
import java.util.stream.Collectors;

public class CalculatorController {
    // FXML widgets from 'calculator-layout.fxml'
    @FXML
    private TextField primary_display;
    @FXML
    private TextField secondary_display;

    // attributes
    double res = 0;
    String s_res = null;
    StringBuilder rolling_value = new StringBuilder();
    String operations = "+-x÷^";

    private LinkedList<String> input = new LinkedList<>();

    private double evaluateExpression(LinkedList<String> tokens) {
        /**
         * Calls the infixToPostfix() and evaluatePostfix() helper methods to
         * return the result of an expression using PEMDAS.
         */
        // convert infix to postfix using Shunting Yard algorithm
        LinkedList<String> postfix = infixToPostfix(tokens);

        // evaluate postfix expression
        return evaluatePostfix(postfix);
    }

    private LinkedList<String> infixToPostfix(LinkedList<String> infix_expression) {
        /**
         * Uses the Shunting Yard algorithm to convert a linked list expression in infix to
         * postfix formatting. Returns the new postfix-formatted linked list.
         */
        // initialize new linked list for storing postfix format
        LinkedList<String> postfix_expression = new LinkedList<>();
        // initialize new stack for storing operators
        Stack<String> operator_stack = new Stack<>();

        // parse original infix-formatted linked list
        for (String token : infix_expression) {
            // add numerical values to the postfix linked list
            if (isNumerical(token)) {
                postfix_expression.add(token);
            // handle operators
            } else if (isOperator(token)) {
                while (!operator_stack.isEmpty() &&
                        isOperator(operator_stack.peek()) &&
                        getPrecedence(operator_stack.peek()) >= getPrecedence(token)) {
                    // remove operator at the top of the stack and add it to the postfix linked list
                    postfix_expression.add(operator_stack.pop());
                }
                // push next operator onto the stack
                operator_stack.push(token);
            }
        }

        while (!operator_stack.isEmpty()) {
            postfix_expression.add(operator_stack.pop());
        }

        return postfix_expression;
    }

    private double evaluatePostfix(LinkedList<String> postfix) {
        /**
         * Evaluates a given postfix expression stored as a linked list.
         */
        Stack<Double> stack = new Stack<>();
        // parse postfix linked list
        for (String token : postfix) {
            // handle numerical values by pushing them onto the stack
            if (isNumerical(token)) {
                stack.push(Double.parseDouble(token));
            // handle invalid expressions by throwing an exception
            } else if (isOperator(token)) {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression");
                }
                // remove the top two elements from the stack
                double y = stack.pop();
                double x = stack.pop();
                // calculate the result
                double result = performOperation(x, y, token);
                stack.push(result);
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression");
        }

        return stack.pop();
    }

    private boolean isNumerical(String token) {
        /**
         * Verifies if a token is numerical.
         */
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isOperator(String token) {
        /**
         * Verifies if a token is an operator.
         */
        return operations.contains(token);
    }

    private int getPrecedence(String operator) {
        /**
         * Uses PEMDAS to determine operator precedence.
         * exponents --> multiplication/division --> addition/subtraction
         *
         * TODO: parenthesis
         */
        switch (operator) {
            case "+":
            case "-":
                return 1;
            case "x":
            case "÷":
                return 2;
            case "^":
                return 3;
            default:
                return -1;
        }
    }

    private double performOperation(double a, double b, String operator) {
        /**
         * Calculates the expression given two values and an operator.
         */
        switch (operator) {
            case "+":
                return a + b;
            case "-":
                return a - b;
            case "x":
                return a * b;
            case "÷":
                // handle dividing by 0
                if (b == 0) {
                    return Double.POSITIVE_INFINITY;
                }
                return a / b;
            case "^":
                return Math.pow(a, b);
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    private String formatResult(double result) {
        /**
         * Formats the result of an expression using BigDecimal to round results
         * to 10 places.
         */
        // handle whole numbers
        if (result == Math.floor(result) && !Double.isInfinite(result) && (result + "").length() <= 10) {
            return String.valueOf((long) result);
        } else {
            // convert to BigDecimal object
            BigDecimal big_res = new BigDecimal(result);
            // create MathContext object with precision of 10 places
            MathContext m = new MathContext(10);
            // round result
            big_res = big_res.round(m).stripTrailingZeros();
            return String.valueOf(big_res);
        }
    }

    private String linkedListToString(LinkedList<String> list) {
        /**
         * Formats a linked list as a string. Removes the brackets and joins the nodes
         * using a single space as a delimiter.
         */
        return (list.stream().collect(Collectors.joining(" ")));
    }

    @FXML
    private void onValueClick(ActionEvent event) {
        /**
         * Left-click binding for numerical buttons.
         */
        if (s_res != null) {
            // clear previous operations
            onClearClick();
        }
        // get number from button text
        String b_text = ((Labeled)event.getSource()).getText();
        // append to current rolling value and insert into linked list
        rolling_value.append(b_text);
        if (!input.isEmpty() && !operations.contains(input.getLast())) {
            input.removeLast();
        }
        input.add(rolling_value.toString());

        // display rolling value in primary text field
        primary_display.setText(rolling_value.toString());
        // display full input (linked list) to secondary text field
        secondary_display.setText(linkedListToString(input));
    }

    @FXML
    private void onOperatorClick(ActionEvent event) {
        /**
         * Left-click binding for operator (e.g., '+', '-', 'x', '÷', '^', 'r') buttons.
         */
        // clear previous operations
        if (s_res != null) {
            onClearClick();
        }
        // handle invalid input
        if (input.isEmpty() || operations.contains(input.getLast())) {
            // do nothing
        } else {
            // get operator from button text
            String b_text = ((Labeled)event.getSource()).getText();

            // handle exponents and roots
            if (b_text.equals("xⁿ")) {
                b_text = "^";
            } else if (b_text.equals("ⁿ√")) {
                b_text = "r";
            }

            // update primary display
            primary_display.setText(rolling_value + " " + b_text);
            // reset rolling value
            rolling_value.setLength(0);
            // add operator to linked list
            input.add(b_text);
            // update secondary display
            secondary_display.setText(linkedListToString(input));
        }
    }

    @FXML
    private void onEqualsClick() {
        /**
         * Left-click binding for equals '=' button.
         */
        if (s_res != null) {
            // clear previous operations
            onClearClick();
        }
        // catch user clicking '=' without any other input
        try {
            // verify the last element is a value
            if (!operations.contains(input.getLast())) {
                // evaluate the expression stored in the linked list
                res = evaluateExpression(input);
                // handle division by 0
                if (res == Double.POSITIVE_INFINITY || res == Double.NEGATIVE_INFINITY) {
                    primary_display.setText("Infinity");
                // handle NaN
                } else if (Double.isNaN(res)) {
                    primary_display.setText("Error");
                } else {
                    // update primary display
                    s_res = formatResult(res);
                    primary_display.setText(s_res);
                    // update secondary display
                    secondary_display.setText(linkedListToString(input) + " = ");
                    // clear res
                    res = 0;
                }
            }
        } catch (Exception e) {
            primary_display.setText("Error");
        }
    }
    @FXML
    private void onSignClick() {
        /**
         * Left-click binding for sign '±' button.
         */
        if (s_res != null) {
            // clear previous operations
            onClearClick();
        }
        // verify rolling value contains a non-zero number
        if (!rolling_value.isEmpty() && !(Double.parseDouble(rolling_value.toString()) == 0.0)) {
            // negate current rolling value
            String n_rolling_value = formatResult(Double.parseDouble(rolling_value.toString()) * -1);
            // reset rolling value to negated value
            rolling_value.setLength(0);
            rolling_value.append(n_rolling_value);
            // update last element of input
            input.removeLast();
            input.add(rolling_value.toString());

            // update primary display
            primary_display.setText(rolling_value.toString());
            // update secondary display
            secondary_display.setText(linkedListToString(input));
        } else {
            // do nothing
        }
    }
    @FXML
    private void onDecimalClick(ActionEvent event) {
        /**
         * Left-click binding for decimal '.' button.
         * Calls the onValueClick() binding --> temporary function for organization purposes.
         */
        onValueClick(event);
    }
    @FXML
    private void onClearClick() {
        /**
         * Left-click binding for clear 'C' button. Clears the primary and
         * secondary displays and expressions.
         */
        primary_display.setText("0");
        secondary_display.setText("");
        s_res = null;
        rolling_value.setLength(0);
        input.clear();
    }
}
