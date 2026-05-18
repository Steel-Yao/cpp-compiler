package org.yyds.lexer;

/**
 * TokenType 枚举定义了词法分析器中使用的各种 token 类型，
 * 包括关键字、标识符、字面量、运算符和分隔符等。
 */
public enum TokenType {
    // 关键字
    INT,
    CHAR,
    FLOAT,
    DOUBLE,
    BOOL,
    VOID,
    IF,
    ELSE,
    WHILE,
    FOR,
    RETURN,
    CLASS,
    PUBLIC,
    PRIVATE,
    PROTECTED,
    TRUE,
    FALSE,

    // 标识符和字面量
    IDENTIFIER,
    INT_LITERAL,
    FLOAT_LITERAL,
    CHAR_LITERAL,
    STRING_LITERAL,

    // 运算符
    /**
     * +
     */
    PLUS,
    /**
     * -
     */
    MINUS,
    /**
     * *
     */
    STAR,
    /**
     * /
     */
    SLASH,
    /**
     * %
     */
    PERCENT,
    /**
     * =
     */
    ASSIGN,
    /**
     * ==
     */
    EQ,
    /**
     * !=
     */
    NEQ,
    /**
     * <
     */
    LT,
    /**
     * >
     */
    GT,
    /**
     * <=
     */
    LE,
    /**
     * >=
     */
    GE,
    /**
     * &&
     */
    AND,
    /**
     * ||
     */
    OR,
    /**
     * !
     */
    NOT,
    /**
     * ++
     */
    INC,
    /**
     * --
     */
    DEC,
    /**
     * +=
     */
    PLUS_ASSIGN,
    /**
     * -=
     */
    MINUS_ASSIGN,
    /**
     * *=
     */
    STAR_ASSIGN,
    /**
     * /=
     */
    SLASH_ASSIGN,
    /**
     * %=
     */
    PERCENT_ASSIGN,

    // 分隔符
    /**
     * (
     */
    LPAREN,
    /**
     * )
     */
    RPAREN,
    /**
     * {
     */
    LBRACE,
    /**
     * }
     */
    RBRACE,
    /**
     * [
     */
    LBRACKET,
    /**
     * ]
     */
    RBRACKET,
    /**
     * ,
     */
    COMMA,
    /**
     * ;
     */
    SEMICOLON,
    /**
     * :
     */
    COLON,
    /**
     * .
     */
    DOT,

    // 其他
    /**
     * 文件结束符
     */
    EOF,
    /**
     * 未知或非法的 token 类型
     */
    UNKNOWN
}
