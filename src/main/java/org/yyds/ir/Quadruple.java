package org.yyds.ir;

/**
 * 四元式中间代码指令，格式为 op、arg1、arg2、result。
 *
 * @param op     操作码
 * @param arg1   第一个操作数
 * @param arg2   第二个操作数
 * @param result 结果位置
 */
public record Quadruple(String op, String arg1, String arg2, String result) {
    @Override
    public String toString() {
        return String.format("(%s, %s, %s, %s)", op, arg1, arg2, result);
    }
}
