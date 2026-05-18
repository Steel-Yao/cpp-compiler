package org.yyds.lexer;

/**
 * 词法分析异常类，用于在词法分析过程中抛出错误信息
 */
public class LexerException extends RuntimeException {
    public LexerException(String message) {
        super(message);
    }
}
