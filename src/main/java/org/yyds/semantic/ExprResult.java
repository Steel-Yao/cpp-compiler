package org.yyds.semantic;

/**
 * 表达式分析结果，place 表示值所在位置，type 表示推导出的类型。
 *
 * @param place 中间代码中保存表达式结果的位置或字面量文本
 * @param type  推导出的语义类型
 */
public record ExprResult(String place, TypeKind type) {
}
