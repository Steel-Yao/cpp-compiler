package org.yyds.parser;

import org.yyds.lexer.TokenType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 根据 FIRST/FOLLOW 集构造 LL(1) 预测分析表。
 */
public class ParseTableBuilder {
    /**
     * 文法
     */
    private final Grammar grammar;
    /**
     * FIRST/FOLLOW 集计算器
     */
    private final FirstFollowCalculator calculator;

    /**
     * 构造函数
     *
     * @param grammar    文法
     * @param calculator FIRST/FOLLOW 集计算器
     */
    public ParseTableBuilder(Grammar grammar, FirstFollowCalculator calculator) {
        this.grammar = grammar;
        this.calculator = calculator;
    }

    /**
     * 构造 LL(1) 预测分析表
     * @return 预测分析表，结构为：非终结符 -> (终结符 -> 产生式)
     */
    public Map<NonTerminal, Map<TokenType, Production>> build() {
        Map<NonTerminal, Map<TokenType, Production>> table = new EnumMap<>(NonTerminal.class);
        for (NonTerminal nonTerminal : NonTerminal.values()) {
            table.put(nonTerminal, new EnumMap<>(TokenType.class));
        }

        for (Production production : grammar.productions()) {
            Set<TokenType> first = calculator.firstOfSequence(production.right());
            for (TokenType terminal : first) {
                put(table, production.left(), terminal, production);
            }

            if (calculator.isNullableSequence(production.right())) {
                for (TokenType terminal : calculator.followSets().get(production.left())) {
                    put(table, production.left(), terminal, production);
                }
            }
        }

        return unmodifiableTable(table);
    }

    /**
     * 将产生式放入预测分析表，并检查冲突
     * @param table 预测分析表
     * @param nonTerminal 非终结符
     * @param terminal 终结符
     * @param production 产生式
     */
    private void put(Map<NonTerminal, Map<TokenType, Production>> table,
                     NonTerminal nonTerminal,
                     TokenType terminal,
                     Production production) {
        Map<TokenType, Production> row = table.get(nonTerminal);
        Production exists = row.get(terminal);
        if (exists == null) {
            row.put(terminal, production);
            return;
        }
        if (exists.equals(production)) {
            return;
        }
        if (production.right().size() == 1 && production.right().getFirst().isEpsilon()) {
            return;
        }
        if (exists.right().size() == 1 && exists.right().getFirst().isEpsilon()) {
            row.put(terminal, production);
            return;
        }
        throw new ParserException("预测分析表冲突：" + nonTerminal + " 在 " + terminal + " 上同时匹配 " + exists + " 和 " + production);
    }

    /**
     * 将预测分析表转换为不可修改的版本，防止后续修改导致错误
     * @param table 可修改的预测分析表
     * @return 不可修改的预测分析表
     */
    private Map<NonTerminal, Map<TokenType, Production>> unmodifiableTable(Map<NonTerminal, Map<TokenType, Production>> table) {
        Map<NonTerminal, Map<TokenType, Production>> result = new EnumMap<>(NonTerminal.class);
        for (Map.Entry<NonTerminal, Map<TokenType, Production>> entry : table.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }
}
