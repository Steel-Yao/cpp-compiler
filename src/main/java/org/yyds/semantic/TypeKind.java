package org.yyds.semantic;

/**
 * 语义分析阶段使用的基础类型。
 */
public enum TypeKind {
    INT,
    FLOAT,
    CHAR,
    BOOL,
    STRING,
    VOID,
    UNKNOWN;

    /**
     * 判断当前类型是否属于数值类型。
     *
     * @return 如果是 {@code INT}、{@code FLOAT} 或 {@code CHAR}，返回 {@code true}。
     */
    public boolean isNumeric() {
        return this == INT || this == FLOAT || this == CHAR;
    }

    /**
     * 判断源类型的值是否可以赋给当前类型。
     *
     * @param source 源类型
     * @return 若两侧类型相同、目标/源存在教学型数值提升关系，或任一方为 {@code UNKNOWN}，则返回 {@code true}
     */
    public boolean canAssignFrom(TypeKind source) {
        if (this == UNKNOWN || source == UNKNOWN) {
            return true;
        }
        if (this == source) {
            return true;
        }
        return this == FLOAT && (source == INT || source == CHAR)
                || this == INT && source == CHAR;
    }
}
