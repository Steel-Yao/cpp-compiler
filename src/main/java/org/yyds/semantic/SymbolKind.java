package org.yyds.semantic;

/**
 * 语义符号的类别，用于区分变量、函数、参数等不同语义实体。
 */
public enum SymbolKind {
    /** 局部变量或全局变量。 */
    VARIABLE,
    /** 函数形参。 */
    PARAMETER,
    /** 函数。 */
    FUNCTION,
    /** 类定义。 */
    CLASS,
    /** 类成员字段。 */
    FIELD
}
