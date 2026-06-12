package org.yyds.optimizer;

import org.yyds.ir.Quadruple;

import java.math.BigDecimal;
import java.util.*;

public class Optimizer {
    private static final Set<String> ARITHMETIC_OPS = Set.of("+", "-", "*", "/", "%");
    private static final Set<String> RELATIONAL_OPS = Set.of("<", "<=", ">", ">=", "==", "!=", "LT", "LE", "GT", "GE", "EQ", "NEQ");
    private static final Set<String> LOGICAL_OPS = Set.of("&&", "||");
    private static final Set<String> UNARY_OPS = Set.of("uminus", "!");
    private static final Set<String> PURE_OPS = union(ARITHMETIC_OPS, RELATIONAL_OPS, LOGICAL_OPS, UNARY_OPS);
    private static final Set<String> COMMUTATIVE_OPS = Set.of("+", "*", "==", "!=", "EQ", "NEQ", "&&", "||");

    public List<Quadruple> optimize(List<Quadruple> input) {
        List<Quadruple> current = new ArrayList<>(input);
        for (int i = 0; i < 3; i++) {
            List<Quadruple> next = optimizeOnce(current);
            if (next.equals(current)) {
                return next;
            }
            current = next;
        }
        return current;
    }
//基本块划分
    public List<BasicBlock> splitBasicBlocks(List<Quadruple> input) {
        if (input.isEmpty()) {
            return List.of();
        }

        Set<Integer> leaders = new LinkedHashSet<>();
        Map<String, Integer> labelIndexes = new HashMap<>();
        leaders.add(0);// 第一条指令是 leader

        for (int i = 0; i < input.size(); i++) {
            Quadruple quad = input.get(i);
            // 标签定义处是 leader
            if (isLabel(quad)) {
                leaders.add(i);
                labelIndexes.put(quad.result(), i);
            }
        }

        for (int i = 0; i < input.size(); i++) {
            Quadruple quad = input.get(i);
            // 跳转指令的下一条是 leader
            if (isJump(quad)) {
                if (i + 1 < input.size()) {
                    leaders.add(i + 1);
                }
                // 跳转目标也是 leader
                Integer target = labelIndexes.get(quad.result());
                if (target != null) {
                    leaders.add(target);
                }
            }
        }
        // 步骤2：根据 leader 划分基本块
        List<Integer> sortedLeaders = leaders.stream().sorted().toList();
        List<BasicBlock> blocks = new ArrayList<>();
        for (int i = 0; i < sortedLeaders.size(); i++) {
            int start = sortedLeaders.get(i);
            int end = i + 1 < sortedLeaders.size() ? sortedLeaders.get(i + 1) : input.size();
            blocks.add(new BasicBlock("B" + (i + 1), input.subList(start, end)));
        }
        return blocks;
    }

    private List<Quadruple> optimizeOnce(List<Quadruple> input) {
        List<Quadruple> optimized = new ArrayList<>();
        for (BasicBlock block : splitBasicBlocks(input)) {
            optimized.addAll(optimizeBlock(block.quadruples()));
        }
        return cleanupJumps(removeDeadCode(optimized));
    }
//公共子表达式消除
    private List<Quadruple> optimizeBlock(List<Quadruple> block) {
        Map<String, String> constants = new HashMap<>();
        Map<String, String> copies = new HashMap<>();
        Map<ExpressionKey, String> expressions = new LinkedHashMap<>();// 表达式缓存：(op, arg1, arg2) → result
        List<Quadruple> result = new ArrayList<>();

        for (Quadruple quad : block) {
            String op = quad.op();
            String arg1 = resolveValue(quad.arg1(), constants, copies);
            String arg2 = resolveValue(quad.arg2(), constants, copies);
            Quadruple optimized = new Quadruple(op, arg1, arg2, quad.result());

            if (PURE_OPS.contains(op)) {
                String folded = fold(op, arg1, arg2);
                if (folded != null) {
                    optimized = new Quadruple("=", folded, null, quad.result());
                    rememberAssignment(optimized, constants, copies, expressions);
                    result.add(optimized);
                    continue;
                }
                // 检查是否已经计算过相同表达式
                ExpressionKey key = new ExpressionKey(op, arg1, arg2);
                String existing = expressions.get(key);
                if (existing != null) {
                    // 复用已有结果
                    optimized = new Quadruple("=", existing, null, quad.result());
                    rememberAssignment(optimized, constants, copies, expressions);
                    result.add(optimized);
                    continue;
                }
                // 记录新表达式
                if (quad.result() != null) {
                    expressions.put(key, quad.result());
                }
                killDefinitions(quad.result(), constants, copies, expressions);
                result.add(optimized);
                continue;
            }

            if ("=".equals(op)) {
                rememberAssignment(optimized, constants, copies, expressions);
                result.add(optimized);
                continue;
            }

            killDefinitions(quad.result(), constants, copies, expressions);
            result.add(optimized);
        }

        return result;
    }

    private void rememberAssignment(Quadruple quad, Map<String, String> constants, Map<String, String> copies, Map<ExpressionKey, String> expressions) {
        killDefinitions(quad.result(), constants, copies, expressions);
        if (quad.result() == null) {
            return;
        }
        if (isLiteral(quad.arg1())) {
            constants.put(quad.result(), quad.arg1());
            return;
        }
        if (isName(quad.arg1())) {
            copies.put(quad.result(), quad.arg1());
        }
    }

    private void killDefinitions(String result, Map<String, String> constants, Map<String, String> copies, Map<ExpressionKey, String> expressions) {
        if (result == null) {
            return;
        }
        constants.remove(result);
        copies.remove(result);
        copies.entrySet().removeIf(entry -> Objects.equals(entry.getValue(), result));
        expressions.entrySet().removeIf(entry -> Objects.equals(entry.getValue(), result) || entry.getKey().contains(result));
    }
//死代码消除
    private List<Quadruple> removeDeadCode(List<Quadruple> input) {
        Set<String> live = new HashSet<>();
        List<Quadruple> reversed = new ArrayList<>();
        // 从后向前遍历
        for (int i = input.size() - 1; i >= 0; i--) {
            Quadruple quad = input.get(i);
            // 如果是纯操作且结果不活跃，则跳过（删除）
            if (isRemovablePureDefinition(quad) && !live.contains(quad.result())) {
                continue;
            }
            // 更新活跃变量集合
            removeDef(live, quad.result());
            addUse(live, quad.arg1());
            addUse(live, quad.arg2());
            if ("jz".equals(quad.op()) || "jnz".equals(quad.op()) || "return".equals(quad.op())) {
                addUse(live, quad.arg1());
            }
            reversed.add(quad);
        }

        List<Quadruple> result = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            result.add(reversed.get(i));
        }
        return result;
    }
//跳转优化
    private List<Quadruple> cleanupJumps(List<Quadruple> input) {
        // 步骤1：解析跳转别名（如 L1: goto L2 → L1 是 L2 的别名）
        Map<String, String> aliases = new HashMap<>();
        for (int i = 0; i + 1 < input.size(); i++) {
            Quadruple current = input.get(i);
            Quadruple next = input.get(i + 1);
            if (isLabel(current) && "goto".equals(next.op())) {
                aliases.put(current.result(), next.result());
            }
        }
        // 步骤2：简化条件跳转
        List<Quadruple> rewritten = new ArrayList<>();
        for (Quadruple quad : input) {
            if (isJump(quad)) {
                rewritten.add(new Quadruple(quad.op(), quad.arg1(), quad.arg2(), resolveLabel(quad.result(), aliases)));
            } else {
                rewritten.add(quad);
            }
        }

        List<Quadruple> simplified = new ArrayList<>();
        for (Quadruple quad : rewritten) {
            // jz true, label → 永远不跳转，删除
            if ("jz".equals(quad.op()) && isBooleanLiteral(quad.arg1())) {
                if (Boolean.parseBoolean(quad.arg1())) {
                    continue;
                }
                simplified.add(new Quadruple("goto", null, null, quad.result()));
                continue;
            }
            if ("jnz".equals(quad.op()) && isBooleanLiteral(quad.arg1())) {
                if (!Boolean.parseBoolean(quad.arg1())) {
                    continue;
                }
                simplified.add(new Quadruple("goto", null, null, quad.result()));
                continue;
            }
            simplified.add(quad);
        }

        List<Quadruple> result = new ArrayList<>();
        for (int i = 0; i < simplified.size(); i++) {
            Quadruple quad = simplified.get(i);
            // goto L1; L1: ... → 直接跳过 goto
            if ("goto".equals(quad.op()) && i + 1 < simplified.size() && isLabel(simplified.get(i + 1)) && Objects.equals(quad.result(), simplified.get(i + 1).result())) {
                continue;
            }
            result.add(quad);
        }
        return result;
    }

    private String resolveValue(String value, Map<String, String> constants, Map<String, String> copies) {
        if (value == null) {
            return null;
        }
        String current = value;
        Set<String> seen = new HashSet<>();
        while (copies.containsKey(current) && seen.add(current)) {
            current = copies.get(current);
        }
        return constants.getOrDefault(current, current);
    }

    private String resolveLabel(String label, Map<String, String> aliases) {
        String current = label;
        Set<String> seen = new HashSet<>();
        while (aliases.containsKey(current) && seen.add(current)) {
            current = aliases.get(current);
        }
        return current;
    }
//常量折叠
    private String fold(String op, String arg1, String arg2) {
        // 一元操作符处理
        if (UNARY_OPS.contains(op)) {
            return foldUnary(op, arg1);
        }
        // 必须两个操作数都是常量
        if (!isLiteral(arg1) || !isLiteral(arg2)) {
            return null;
        }
        // 布尔常量折叠
        if (isBooleanLiteral(arg1) || isBooleanLiteral(arg2)) {
            return foldBoolean(op, arg1, arg2);
        }
        if (!isNumericLiteral(arg1) || !isNumericLiteral(arg2)) {
            return null;
        }
        if ("%".equals(op) && (!isIntegerLiteral(arg1) || !isIntegerLiteral(arg2))) {
            return null;
        }
        // 数值常量折叠
        BigDecimal left = new BigDecimal(arg1);
        BigDecimal right = new BigDecimal(arg2);
        return switch (op) {
            case "+" -> format(left.add(right));
            case "-" -> format(left.subtract(right));
            case "*" -> format(left.multiply(right));
            case "/" -> right.compareTo(BigDecimal.ZERO) == 0 ? null : format(left.divide(right, 10, java.math.RoundingMode.HALF_UP));
            case "%" -> right.compareTo(BigDecimal.ZERO) == 0 ? null : format(new BigDecimal(left.toBigInteger().remainder(right.toBigInteger())));
            case "<", "LT" -> bool(left.compareTo(right) < 0);
            case "<=", "LE" -> bool(left.compareTo(right) <= 0);
            case ">", "GT" -> bool(left.compareTo(right) > 0);
            case ">=", "GE" -> bool(left.compareTo(right) >= 0);
            case "==", "EQ" -> bool(left.compareTo(right) == 0);
            case "!=", "NEQ" -> bool(left.compareTo(right) != 0);
            default -> null;
        };
    }

    private String foldUnary(String op, String arg) {
        if ("!".equals(op) && isBooleanLiteral(arg)) {
            return bool(!Boolean.parseBoolean(arg));
        }
        if ("uminus".equals(op) && isNumericLiteral(arg)) {
            return format(new BigDecimal(arg).negate());
        }
        return null;
    }

    private String foldBoolean(String op, String arg1, String arg2) {
        if (!isBooleanLiteral(arg1) || !isBooleanLiteral(arg2)) {
            return null;
        }
        boolean left = Boolean.parseBoolean(arg1);
        boolean right = Boolean.parseBoolean(arg2);
        return switch (op) {
            case "&&" -> bool(left && right);
            case "||" -> bool(left || right);
            case "==", "EQ" -> bool(left == right);
            case "!=", "NEQ" -> bool(left != right);
            default -> null;
        };
    }

    private boolean isRemovablePureDefinition(Quadruple quad) {
        return quad.result() != null && (PURE_OPS.contains(quad.op()) || "=".equals(quad.op()) && isTemp(quad.result()));
    }

    private boolean isJump(Quadruple quad) {
        return "goto".equals(quad.op()) || "jz".equals(quad.op()) || "jnz".equals(quad.op());
    }

    private boolean isLabel(Quadruple quad) {
        return "label".equals(quad.op());
    }

    private boolean isTemp(String value) {
        return value != null && value.matches("t\\d+");
    }

    private boolean isName(String value) {
        return value != null && !isLiteral(value) && !"-".equals(value);
    }

    private boolean isLiteral(String value) {
        return isNumericLiteral(value) || isBooleanLiteral(value) || isQuotedLiteral(value);
    }

    private boolean isNumericLiteral(String value) {
        return value != null && value.matches("-?\\d+(\\.\\d+)?");
    }

    private boolean isIntegerLiteral(String value) {
        return value != null && value.matches("-?\\d+");
    }

    private boolean isBooleanLiteral(String value) {
        return "true".equals(value) || "false".equals(value);
    }

    private boolean isQuotedLiteral(String value) {
        return value != null && (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"") || value.length() >= 3 && value.startsWith("'") && value.endsWith("'"));
    }

    private void addUse(Set<String> live, String value) {
        if (isName(value)) {
            live.add(value);
        }
    }

    private void removeDef(Set<String> live, String value) {
        if (value != null) {
            live.remove(value);
        }
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String bool(boolean value) {
        return Boolean.toString(value);
    }

    @SafeVarargs
    private static Set<String> union(Set<String>... sets) {
        Set<String> result = new HashSet<>();
        for (Set<String> set : sets) {
            result.addAll(set);
        }
        return result;
    }

    private record ExpressionKey(String op, String arg1, String arg2) {
        private ExpressionKey {
            // 对于交换律操作符，统一参数顺序
            if (COMMUTATIVE_OPS.contains(op) && arg1 != null && arg2 != null && Comparator.nullsFirst(String::compareTo).compare(arg2, arg1) < 0) {
                String temp = arg1;
                arg1 = arg2;
                arg2 = temp;
            }
        }

        private boolean contains(String value) {
            return Objects.equals(arg1, value) || Objects.equals(arg2, value);
        }
    }
}
