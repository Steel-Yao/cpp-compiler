package org.yyds.codegen;

import java.util.List;

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
