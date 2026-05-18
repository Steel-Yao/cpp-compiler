package org.yyds.codegen;

import org.yyds.ir.Quadruple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CodeGenerator {
    private static final String WORK_REGISTER = "R1";
    private static final Set<String> BINARY_OPS = Set.of("+", "-", "*", "/", "%", "&&", "||", "<", "<=", ">", ">=", "==", "!=", "LT", "LE", "GT", "GE", "EQ", "NEQ");
    private static final Set<String> UNARY_OPS = Set.of("uminus", "!");

    public List<TargetInstruction> generate(List<Quadruple> quadruples) {
        List<TargetInstruction> instructions = new ArrayList<>();
        for (Quadruple quadruple : quadruples) {
            generateOne(quadruple, instructions);
        }
        return instructions;
    }

    private void generateOne(Quadruple quadruple, List<TargetInstruction> instructions) {
        String op = quadruple.op();
        if ("=".equals(op)) {
            generateAssign(quadruple, instructions);
            return;
        }
        if (BINARY_OPS.contains(op)) {
            generateBinary(quadruple, instructions);
            return;
        }
        if (UNARY_OPS.contains(op)) {
            generateUnary(quadruple, instructions);
            return;
        }
        switch (op) {
            case "label" -> generateLabel(quadruple, instructions);
            case "goto" -> generateGoto(quadruple, instructions);
            case "jz" -> generateConditionalJump("JZ", quadruple, instructions);
            case "jnz" -> generateConditionalJump("JNZ", quadruple, instructions);
            case "return" -> generateReturn(quadruple, instructions);
            default -> throw error("不支持的目标代码操作码：" + op, quadruple);
        }
    }

    private void generateAssign(Quadruple quadruple, List<TargetInstruction> instructions) {
        requireArg1(quadruple);
        requireResult(quadruple);
        emit(instructions, "LOAD", WORK_REGISTER, quadruple.arg1());
        emit(instructions, "STORE", quadruple.result(), WORK_REGISTER);
    }

    private void generateBinary(Quadruple quadruple, List<TargetInstruction> instructions) {
        requireArg1(quadruple);
        requireArg2(quadruple);
        requireResult(quadruple);
        emit(instructions, "LOAD", WORK_REGISTER, quadruple.arg1());
        emit(instructions, mapBinaryOp(quadruple.op()), WORK_REGISTER, quadruple.arg2());
        emit(instructions, "STORE", quadruple.result(), WORK_REGISTER);
    }

    private void generateUnary(Quadruple quadruple, List<TargetInstruction> instructions) {
        requireArg1(quadruple);
        requireResult(quadruple);
        emit(instructions, "LOAD", WORK_REGISTER, quadruple.arg1());
        emit(instructions, mapUnaryOp(quadruple.op()), WORK_REGISTER);
        emit(instructions, "STORE", quadruple.result(), WORK_REGISTER);
    }

    private void generateLabel(Quadruple quadruple, List<TargetInstruction> instructions) {
        requireResult(quadruple);
        emit(instructions, "LABEL", quadruple.result());
    }

    private void generateGoto(Quadruple quadruple, List<TargetInstruction> instructions) {
        requireResult(quadruple);
        emit(instructions, "JMP", quadruple.result());
    }

    private void generateConditionalJump(String op, Quadruple quadruple, List<TargetInstruction> instructions) {
        requireArg1(quadruple);
        requireResult(quadruple);
        emit(instructions, "LOAD", WORK_REGISTER, quadruple.arg1());
        emit(instructions, op, WORK_REGISTER, quadruple.result());
    }

    private void generateReturn(Quadruple quadruple, List<TargetInstruction> instructions) {
        if (isBlank(quadruple.arg1())) {
            emit(instructions, "RET");
            return;
        }
        emit(instructions, "LOAD", WORK_REGISTER, quadruple.arg1());
        emit(instructions, "RET", WORK_REGISTER);
    }

    private String mapBinaryOp(String op) {
        return switch (op) {
            case "+" -> "ADD";
            case "-" -> "SUB";
            case "*" -> "MUL";
            case "/" -> "DIV";
            case "%" -> "MOD";
            case "&&" -> "AND";
            case "||" -> "OR";
            case "<", "LT" -> "CMP_LT";
            case "<=", "LE" -> "CMP_LE";
            case ">", "GT" -> "CMP_GT";
            case ">=", "GE" -> "CMP_GE";
            case "==", "EQ" -> "CMP_EQ";
            case "!=", "NEQ" -> "CMP_NE";
            default -> throw new CodeGenerationException("不支持的二元操作码：" + op);
        };
    }

    private String mapUnaryOp(String op) {
        return switch (op) {
            case "uminus" -> "NEG";
            case "!" -> "NOT";
            default -> throw new CodeGenerationException("不支持的一元操作码：" + op);
        };
    }

    private void requireArg1(Quadruple quadruple) {
        if (isBlank(quadruple.arg1())) {
            throw error("四元式缺少第一个操作数", quadruple);
        }
    }

    private void requireArg2(Quadruple quadruple) {
        if (isBlank(quadruple.arg2())) {
            throw error("四元式缺少第二个操作数", quadruple);
        }
    }

    private void requireResult(Quadruple quadruple) {
        if (isBlank(quadruple.result())) {
            throw error("四元式缺少结果或跳转目标", quadruple);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank() || "-".equals(value);
    }

    private void emit(List<TargetInstruction> instructions, String op, String... operands) {
        instructions.add(new TargetInstruction(op, List.of(operands)));
    }

    private CodeGenerationException error(String message, Quadruple quadruple) {
        return new CodeGenerationException(message + "：" + quadruple);
    }
}
