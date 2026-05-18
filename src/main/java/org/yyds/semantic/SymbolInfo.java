package org.yyds.semantic;

import java.util.List;

/**
 * 语义符号表项，保存标识符在语义阶段需要使用的信息。
 */
public record SymbolInfo(
        String name,
        TypeKind type,
        SymbolKind kind,
        int line,
        int column,
        List<TypeKind> parameterTypes,
        TypeKind returnType
) {
    /**
     * 创建一个简化的符号条目，适用于变量、参数和类字段。
     *
     * @param name   符号名
     * @param type   符号类型
     * @param kind   符号类别
     * @param line   声明行号
     * @param column 声明列号
     */
    public SymbolInfo(String name, TypeKind type, SymbolKind kind, int line, int column) {
        this(name, type, kind, line, column, List.of(), type);
    }
}
