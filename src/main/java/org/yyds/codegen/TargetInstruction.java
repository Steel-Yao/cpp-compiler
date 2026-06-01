package org.yyds.codegen;

import java.util.List;

/**
 * 目标代码生成阶段产出的单条伪汇编指令。
 *
 * @param op       指令助记符，如 {@code LOAD}、{@code STORE}、{@code JMP}
 * @param operands 指令操作数，构造时会复制为不可变列表
 */
public record TargetInstruction(String op, List<String> operands) {
    public TargetInstruction {
        operands = List.copyOf(operands);
    }

    @Override
    public String toString() {
        if (operands.isEmpty()) {
            return op;
        }
        return op + " " + String.join(", ", operands);
    }
}
