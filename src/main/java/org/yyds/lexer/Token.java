package org.yyds.lexer;

/**
 * Token 记录了词法分析器识别出的每个词素的信息
 *
 * @param type   TokenType 枚举值，表示词素的类型（如关键字、标识符、常量、运算符等）
 * @param lexeme 词素的原始文本内容
 * @param line   词素在源代码中的行号，从 1 开始
 * @param column 词素在源代码中的列号，从 1 开始，表示该词素在行中的起始位置
 * @see TokenType
 */
public record Token(TokenType type, String lexeme, int line, int column) {
    @Override
    public String toString() {
        return String.format("%-16s %-12s (%d, %d)", type, lexeme, line, column);
    }
}
