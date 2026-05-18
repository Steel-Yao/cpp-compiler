package org.yyds.parser;

import org.yyds.lexer.TokenType;

import java.util.*;

/**
 * 计算文法的 FIRST 集与 FOLLOW 集。
 */
public class FirstFollowCalculator {
    /**
     * 文法定义
     */
    private final Grammar grammar;
    /**
     * FIRST 集
     */
    private final Map<NonTerminal, Set<TokenType>> firstSets = new EnumMap<>(NonTerminal.class);
    /**
     * FOLLOW 集
     */
    private final Map<NonTerminal, Set<TokenType>> followSets = new EnumMap<>(NonTerminal.class);
    /**
     * 可空非终结符集合
     */
    private final Set<NonTerminal> nullableSet = EnumSet.noneOf(NonTerminal.class);

    /**
     * 构造函数，接受一个文法定义并计算 FIRST 集与 FOLLOW 集。
     *
     * @param grammar 文法定义
     */
    public FirstFollowCalculator(Grammar grammar) {
        this.grammar = grammar;
        for (NonTerminal nonTerminal : NonTerminal.values()) {
            firstSets.put(nonTerminal, EnumSet.noneOf(TokenType.class));
            followSets.put(nonTerminal, EnumSet.noneOf(TokenType.class));
        }
        computeFirstSets();
        computeFollowSets();
    }

    /**
     * 获取 FIRST 集的不可修改副本，防止外部修改。
     *
     * @return FIRST 集的不可修改副本
     */
    public Map<NonTerminal, Set<TokenType>> firstSets() {
        return unmodifiableCopy(firstSets);
    }

    /**
     * 获取 FOLLOW 集的不可修改副本，防止外部修改。
     *
     * @return FOLLOW 集的不可修改副本
     */
    public Map<NonTerminal, Set<TokenType>> followSets() {
        return unmodifiableCopy(followSets);
    }

    /**
     * 计算一个符号序列的 FIRST 集。<br>
     * 1. 从左到右扫描符号序列：<br>
     * - 如果遇到一个终结符 a，则 FIRST(序列) = {a}，停止扫描。<br>
     * - 如果遇到一个非终结符 A，则将 FIRST(A) 中的所有非 ε 的符号加入 FIRST(序列)。<br>
     * - 如果 FIRST(A) 包含 ε，则继续扫描下一个符号；否则停止扫描。<br>
     * 2. 如果序列中的所有符号都可空（即 FIRST(序列) 包含 ε），则将 ε 加入 FIRST(序列)。
     *
     * @param symbols 符号序列
     * @return 符号序列的 FIRST 集
     */
    public Set<TokenType> firstOfSequence(List<GrammarSymbol> symbols) {
        Set<TokenType> result = EnumSet.noneOf(TokenType.class);
        if (symbols.isEmpty()) {
            return result;
        }
        for (GrammarSymbol symbol : symbols) {
            if (symbol.isEpsilon()) {
                continue;
            }
            if (symbol.isTerminal()) { // 如果是终结符
                // 直接加入 FIRST 集并停止扫描
                result.add(symbol.terminal());
                return result;
            }
            // 如果是非终结符，将 FIRST(B) 中的所有非 ε 的符号加入 FIRST(序列)
            result.addAll(firstSets.get(symbol.nonTerminal()));
            if (!nullableSet.contains(symbol.nonTerminal())) { // 如果 FIRST(B) 不包含 ε
                // 停止扫描
                return result;
            }
        }
        return result;
    }

    /**
     * 判断一个符号序列是否可空（即是否可以推导出 ε）。
     *
     * @param symbols 符号序列
     * @return {@code true} 如果序列可空，否则 {@code false}
     */
    public boolean isNullableSequence(List<GrammarSymbol> symbols) {
        return symbols.isEmpty() || symbols.stream().allMatch(symbol -> symbol.isEpsilon()
                || symbol.isNonTerminal() && nullableSet.contains(symbol.nonTerminal()));
    }

    /**
     * 计算 FIRST 集，使用迭代方法直到不再有变化。<br>
     * 1. 对于每个产生式 A → α：<br>
     * - 从左到右扫描 α 中的符号：<br>
     * - 如果是终结符 a，则将 a 加入 FIRST(A)，并停止扫描。<br>
     * - 如果是非终结符 B，则将 FIRST(B) 中的所有非 ε 的符号加入 FIRST(A)。<br>
     * - 如果 FIRST(B) 包含 ε，则继续扫描下一个符号；否则停止扫描。<br>
     * - 如果 α 中的所有符号都可空（即 FIRST(α) 包含 ε），则将 ε 加入 FIRST(A)。<br>
     * 2. 重复以上步骤直到 FIRST 集不再发生变化。
     */
    private void computeFirstSets() {
        boolean changed;
        do {
            changed = false;
            for (Production production : grammar.productions()) {
                NonTerminal left = production.left();
                List<GrammarSymbol> right = production.right();
                boolean nullable = true; // 标记当前产生式右侧是否可空

                for (GrammarSymbol symbol : right) {
                    if (symbol.isEpsilon()) {
                        continue;
                    }
                    if (symbol.isTerminal()) { // 如果是终结符
                        // 直接加入 FIRST 集并停止扫描
                        changed |= firstSets.get(left).add(symbol.terminal());
                        nullable = false;
                        break;
                    }
                    // 如果是非终结符，将 FIRST(B) 中的所有非 ε 的符号加入 FIRST(A)
                    changed |= firstSets.get(left).addAll(firstSets.get(symbol.nonTerminal()));
                    if (!nullableSet.contains(symbol.nonTerminal())) {
                        nullable = false;
                        break;
                    }
                }

                if (nullable) { // 如果右侧所有符号都可空
                    // 将左侧非终结符加入可空集合
                    changed |= nullableSet.add(left);
                }
            }
        } while (changed);
    }

    /**
     * 计算 FOLLOW 集，使用迭代方法直到不再有变化。<br>
     * 1. 对于每个产生式 A → αBβ：<br>
     * - 将 FIRST(β) 中的所有非 ε 的符号加入 FOLLOW(B)。<br>
     * - 如果 β 可空（即 FIRST(β) 包含 ε 或 β 为空），则将 FOLLOW(A) 中的所有符号加入 FOLLOW(B)。<br>
     * 2. 对于每个产生式 A → αB：<br>
     * - 将 FOLLOW(A) 中的所有符号加入 FOLLOW(B)。<br>
     * 3. 对于文法的开始符号 S，将 EOF 加入 FOLLOW(S)。<br>
     * 4. 重复以上步骤直到 FOLLOW 集不再发生变化。
     */
    private void computeFollowSets() {
        followSets.get(grammar.startSymbol()).add(TokenType.EOF);
        boolean changed;
        do {
            changed = false;
            for (Production production : grammar.productions()) {
                List<GrammarSymbol> right = production.right();
                for (int i = 0; i < right.size(); i++) {
                    GrammarSymbol symbol = right.get(i);
                    if (!symbol.isNonTerminal()) {
                        continue;
                    }
                    // 计算 FIRST(β)，其中 β 是 symbol 之后的符号序列
                    List<GrammarSymbol> suffix = new ArrayList<>(right.subList(i + 1, right.size()));
                    // 将 FIRST(β) 中的所有非 ε 的符号加入 FOLLOW(B)
                    changed |= followSets.get(symbol.nonTerminal()).addAll(firstOfSequence(suffix));
                    if (suffix.isEmpty() || isNullableSequence(suffix)) { // 如果 β 可空
                        // 将 FOLLOW(A) 中的所有符号加入 FOLLOW(B)
                        changed |= followSets.get(symbol.nonTerminal()).addAll(followSets.get(production.left()));
                    }
                }
            }
        } while (changed);
    }

    /**
     * 创建一个不可修改的 FIRST/FOLLOW 集副本，防止外部修改。
     *
     * @param source 原始 FIRST/FOLLOW 集
     * @return 不可修改的 FIRST/FOLLOW 集副本
     */
    private Map<NonTerminal, Set<TokenType>> unmodifiableCopy(Map<NonTerminal, Set<TokenType>> source) {
        Map<NonTerminal, Set<TokenType>> result = new EnumMap<>(NonTerminal.class);
        for (Map.Entry<NonTerminal, Set<TokenType>> entry : source.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }
}
