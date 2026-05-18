package org.yyds.lexer;

/**
 * 标识符和常量的符号表项
 * 包含词素、类型、首次出现的位置（行号和列号）以及出现次数等信息
 */
public class Symbol {
    /**
     * 词素（lexeme）是标识符或常量的具体文本内容，例如变量名、字符串字面量等。
     */
    private final String lexeme;
    /**
     * 类型（type）表示该符号是标识符还是常量，以及具体的类型信息，例如 int、float、string 等。
     *
     * @see TokenType
     */
    private final TokenType type;
    /**
     * 首次出现的位置（行号）
     */
    private final int line;
    /**
     * 首次出现的位置（列号）
     */
    private final int column;
    /**
     * 出现次数（occurrences）记录该符号在源代码中出现的总次数，初始值为 1，每次再次遇到该符号时增加 1。
     */
    private int occurrences;

    /**
     * 构造函数
     * @param lexeme 词素
     * @param type 类型
     * @param line 首次出现的行号
     * @param column 首次出现的列号
     */
    public Symbol(String lexeme, TokenType type, int line, int column) {
        this.lexeme = lexeme;
        this.type = type;
        this.line = line;
        this.column = column;
        this.occurrences = 1;
    }

    public String getLexeme() {
        return lexeme;
    }

    public TokenType getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void increaseOccurrences() {
        occurrences++;
    }

    @Override
    public String toString() {
        return String.format("%-12s %-16s first=(%d, %d) count=%d", lexeme, type, line, column, occurrences);
    }
}
