package org.yyds.semantic;

/**
 * 语义分析阶段抛出的异常，用于报告重复定义、未声明引用和类型不匹配等错误。
 */
public class SemanticException extends RuntimeException {
    /**
     * 创建一个语义异常。
     */
    public SemanticException(String message) {
        super(message);
    }
}
