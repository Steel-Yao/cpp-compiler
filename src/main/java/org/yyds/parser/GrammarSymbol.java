package org.yyds.parser;

import org.yyds.lexer.TokenType;

/**
 * 统一表示终结符、非终结符和空串，便于产生式和分析栈复用同一套结构。
 *
 * @param kind        符号的种类，区分终结符、非终结符和空串。
 * @param terminal    终结符对应的 {@link TokenType} 枚举值，仅当 {@code kind} 为 {@code TERMINAL} 时有效。
 * @param nonTerminal 非终结符对应的 {@link NonTerminal} 枚举值，仅当 {@code kind} 为 {@code NON_TERMINAL} 时有效。
 */
public record GrammarSymbol(Kind kind, TokenType terminal, NonTerminal nonTerminal) {
    /**
     * 空串符号，表示某个非终结符可以推导出空串，在分析栈中不占位置。
     */
    public static final GrammarSymbol EPSILON = new GrammarSymbol(Kind.EPSILON, null, null);

    /**
     * 创建一个终结符号，直接对应词法分析得到的 TokenType。
     *
     * @param terminal 终结符对应的 TokenType 枚举值。
     * @return 一个表示终结符的 GrammarSymbol 实例。
     */
    public static GrammarSymbol terminal(TokenType terminal) {
        return new GrammarSymbol(Kind.TERMINAL, terminal, null);
    }

    /**
     * 创建一个非终结符号，文法中定义的符号，用于构造语法分析树。
     *
     * @param nonTerminal 非终结符对应的 NonTerminal 枚举值。
     * @return 一个表示非终结符的 GrammarSymbol 实例。
     */
    public static GrammarSymbol nonTerminal(NonTerminal nonTerminal) {
        return new GrammarSymbol(Kind.NON_TERMINAL, null, nonTerminal);
    }

    public boolean isTerminal() {
        return kind == Kind.TERMINAL;
    }

    public boolean isNonTerminal() {
        return kind == Kind.NON_TERMINAL;
    }

    public boolean isEpsilon() {
        return kind == Kind.EPSILON;
    }

    public String displayName() {
        return switch (kind) {
            case TERMINAL -> terminal.name();
            case NON_TERMINAL -> nonTerminal.name();
            case EPSILON -> "ε";
        };
    }

    /**
     * 语法符号的种类，区分终结符、非终结符和空串。
     */
    public enum Kind {
        /**
         * 终结符，直接对应词法分析得到的 TokenType。
         */
        TERMINAL,
        /**
         * 非终结符，文法中定义的符号，用于构造语法分析树。
         */
        NON_TERMINAL,
        /**
         * 空串，表示某个非终结符可以推导出空串，在分析栈中不占位置。
         */
        EPSILON
    }
}
