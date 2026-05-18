package org.yyds.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中间代码生成器，负责维护四元式列表、临时变量和标签编号。
 */
public class IRGenerator {
    /**
     * 已生成的四元式序列。
     */
    private final List<Quadruple> quadruples = new ArrayList<>();
    /**
     * 下一次分配的临时变量编号。
     */
    private int tempIndex = 1;
    /**
     * 下一次分配的标签编号。
     */
    private int labelIndex = 1;

    /**
     * 分配一个新的临时变量名。
     *
     * @return 形如 {@code t1}、{@code t2} 的临时变量名
     */
    public String newTemp() {
        return "t" + tempIndex++;
    }

    /**
     * 分配一个新的跳转标签名。
     *
     * @return 形如 {@code L1}、{@code L2} 的标签名
     */
    public String newLabel() {
        return "L" + labelIndex++;
    }

    /**
     * 追加一条四元式。
     *
     * @param op     操作码
     * @param arg1   第一个操作数
     * @param arg2   第二个操作数
     * @param result 结果位置
     */
    public void emit(String op, String arg1, String arg2, String result) {
        quadruples.add(new Quadruple(op, arg1, arg2, result));
    }

    /**
     * 获取不可变的四元式列表视图。
     */
    public List<Quadruple> getQuadruples() {
        return Collections.unmodifiableList(quadruples);
    }
}
