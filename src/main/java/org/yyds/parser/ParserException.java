package org.yyds.parser;

/**
 * 语法分析异常类，用于在预测分析过程中报告文法不匹配、表项缺失等错误。
 */
public class ParserException extends RuntimeException {
    public ParserException(String message) {
        super(message);
    }
}
