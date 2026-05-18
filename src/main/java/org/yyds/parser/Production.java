package org.yyds.parser;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 产生式，左边是一个非终结符，右边是一个符号序列（可以包含终结符和非终结符）。
 * @param left 非终结符
 * @param right 右边的符号序列
 */
public record Production(NonTerminal left, List<GrammarSymbol> right) {
    @Override
    public String toString() {
        String body = right.stream()
                .map(GrammarSymbol::displayName)
                .collect(Collectors.joining(" "));
        return left.name() + " -> " + body;
    }
}
