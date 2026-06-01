package org.yyds.parser;

import org.yyds.lexer.Token;
import org.yyds.lexer.TokenType;

import java.util.*;

/**
 * 表驱动预测分析器，负责把 Token 序列转换为语法分析树。
 */
public class Parser {
    private final Grammar grammar;
    private final FirstFollowCalculator firstFollowCalculator;
    private final Map<NonTerminal, Map<TokenType, Production>> parseTable;

    /**
     * 创建预测分析器，并立即根据内置文法构造 FIRST/FOLLOW 集和 LL(1) 分析表。
     */
    public Parser() {
        this.grammar = new Grammar();
        this.firstFollowCalculator = new FirstFollowCalculator(grammar);
        this.parseTable = new ParseTableBuilder(grammar, firstFollowCalculator).build();
    }

    /**
     * 使用表驱动 LL(1) 算法将 Token 序列转换为语法树。
     *
     * @param tokens 词法分析阶段生成的 Token 序列；末尾可以省略 EOF
     * @return 以 PROGRAM 为根的语法树
     * @throws ParserException 当输入 Token 不能匹配当前文法时抛出
     */
    public ParseTreeNode parse(List<Token> tokens) {
        List<Token> input = ensureEof(tokens);
        int position = 0;

        Deque<GrammarSymbol> symbolStack = new ArrayDeque<>();
        Deque<ParseTreeNode> nodeStack = new ArrayDeque<>();
        ParseTreeNode root = new ParseTreeNode(grammar.startSymbol().name());

        symbolStack.push(GrammarSymbol.nonTerminal(grammar.startSymbol()));
        nodeStack.push(root);

        while (!symbolStack.isEmpty()) {
            GrammarSymbol top = symbolStack.pop();
            ParseTreeNode currentNode = nodeStack.pop();
            Token lookahead = input.get(Math.min(position, input.size() - 1));

            if (top.isEpsilon()) {
                continue;
            }
            if (top.isTerminal()) {
                if (top.terminal() != lookahead.type()) {
                    throw error("期望 " + top.terminal() + "，实际得到 " + lookahead.type(), lookahead);
                }
                currentNode.setToken(lookahead);
                position++;
                continue;
            }

            Production production = parseTable.get(top.nonTerminal()).get(lookahead.type());
            if (production == null) {
                throw error("无法根据非终结符 " + top.nonTerminal() + " 和输入符号 " + lookahead.type() + " 选择产生式", lookahead);
            }

            List<ParseTreeNode> children = new ArrayList<>();
            for (GrammarSymbol symbol : production.right()) {
                ParseTreeNode child = new ParseTreeNode(symbol.displayName());
                currentNode.addChild(child);
                children.add(child);
            }

            List<GrammarSymbol> right = production.right();
            for (int i = right.size() - 1; i >= 0; i--) {
                GrammarSymbol symbol = right.get(i);
                if (symbol.isEpsilon()) {
                    continue;
                }
                symbolStack.push(symbol);
                nodeStack.push(children.get(i));
            }
        }

        if (position < input.size() - 1) {
            throw error("语法分析结束后仍有未消费的 Token", input.get(position));
        }
        return root;
    }

    /**
     * 获取内置文法的 FIRST 集，供教学展示和调试分析表时使用。
     *
     * @return 非终结符到 FIRST 终结符集合的映射
     */
    public Map<NonTerminal, Set<TokenType>> firstSets() {
        return firstFollowCalculator.firstSets();
    }

    /**
     * 获取内置文法的 FOLLOW 集，供教学展示和调试分析表时使用。
     *
     * @return 非终结符到 FOLLOW 终结符集合的映射
     */
    public Map<NonTerminal, Set<TokenType>> followSets() {
        return firstFollowCalculator.followSets();
    }

    /**
     * 获取根据 FIRST/FOLLOW 集构造出的 LL(1) 预测分析表。
     *
     * @return 非终结符和展望 Token 到产生式的映射
     */
    public Map<NonTerminal, Map<TokenType, Production>> parseTable() {
        return parseTable;
    }

    private List<Token> ensureEof(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of(new Token(TokenType.EOF, "", 1, 1));
        }
        Token last = tokens.getLast();
        if (last.type() == TokenType.EOF) {
            return tokens;
        }
        List<Token> result = new ArrayList<>(tokens);
        result.add(new Token(TokenType.EOF, "", last.line(), last.column() + last.lexeme().length()));
        return Collections.unmodifiableList(result);
    }

    private ParserException error(String message, Token token) {
        return new ParserException(message + "，位置：第 " + token.line() + " 行，第 " + token.column() + " 列，词素：" + token.lexeme());
    }
}
