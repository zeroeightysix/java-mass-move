package me.zeroeightsix.jmm;

import java.util.function.UnaryOperator;

public enum ReferenceProcessor {

    REMOVE_SNAKE_CASE,
    TITLE_CASE,
    NORMAL_CASE,
    LOWER_CASE_FIRST,
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
    private static final UnaryOperator<String> lowerFirst = s -> {
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
        REMOVE_SNAKE_CASE.unaryOperator = removeSnake;
        TITLE_CASE.unaryOperator = toTitleCase;
        NORMAL_CASE.unaryOperator = toNormalCase;
        LOWER_CASE_FIRST.unaryOperator = lowerFirst;
        UPPERCASE.unaryOperator = String::toUpperCase;
        LOWERCASE.unaryOperator = String::toLowerCase;
        REMOVE_SPACES.unaryOperator = s -> s.replaceAll(" ", "");
    }

}
