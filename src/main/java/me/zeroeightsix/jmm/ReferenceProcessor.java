package me.zeroeightsix.jmm;

import java.util.function.UnaryOperator;

public enum ReferenceProcessor {

    SNAKE_CASE_TO_TITLE_CASE,
    SNAKE_CASE_TO_NORMAL_CASE,
    TITLE_CASE_TO_NORMAL_CASE,
    UPPERCASE,
    LOWERCASE,
    REMOVE_SPACES;

    private static final UnaryOperator<String> removeSnake = s -> s.replace('_', ' ');
    private static final UnaryOperator<String> toTitleCase = s -> {
        boolean space = true;
        StringBuilder builder = new StringBuilder();
        for (char c : s.toCharArray()) {
            builder.append(space ? Character.toUpperCase(c) : c);
            space = c == ' ';
        }
        return builder.toString();
    };
    private static final UnaryOperator<String> titleToLower = s -> {
        boolean space = true;
        StringBuilder builder = new StringBuilder();
        for (char c : s.toCharArray()) {
            builder.append(space ? Character.toLowerCase(c) : c);
            space = c == ' ';
        }
        return builder.toString();
    };
    private static final UnaryOperator<String> toNormalCase = s -> s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);

    private static UnaryOperator<String> merge(UnaryOperator<String>... operators) {
        return s -> {
            for (UnaryOperator<String> operator : operators) s = operator.apply(s);
            return s;
        };
    }

    UnaryOperator<String> unaryOperator;

    static {
        SNAKE_CASE_TO_TITLE_CASE.unaryOperator = merge(removeSnake, toTitleCase);
        SNAKE_CASE_TO_NORMAL_CASE.unaryOperator = merge(removeSnake, toNormalCase);
        TITLE_CASE_TO_NORMAL_CASE.unaryOperator = merge(titleToLower, toNormalCase);
        UPPERCASE.unaryOperator = String::toUpperCase;
        LOWERCASE.unaryOperator = String::toLowerCase;
        REMOVE_SPACES.unaryOperator = s -> s.replaceAll(" ", "");
    }

}
