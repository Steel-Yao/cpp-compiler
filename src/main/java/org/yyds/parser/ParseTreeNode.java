package org.yyds.parser;

import org.yyds.lexer.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 语法分析树节点。
 * 叶子节点保存实际匹配到的 Token，非叶子节点保存文法符号名。
 */
public class ParseTreeNode {
    /**
     * 文法符号的名字，非叶子节点保存非终结符名字，叶子节点保存终结符名字。
     */
    private final String symbol;
    /**
     * 子节点列表，按照文法产生式右部符号的顺序保存。叶子节点的 {@code children} 列表为空。
     */
    private final List<ParseTreeNode> children = new ArrayList<>();
    /**
     * 叶子节点保存实际匹配到的 Token，非叶子节点 token 字段为 {@code null}。
     */
    private Token token;

    /**
     * 构造一个 ParseTreeNode
     *
     * @param symbol 文法符号名字，非叶子节点保存非终结符名字，叶子节点保存终结符名字。
     */
    public ParseTreeNode(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public List<ParseTreeNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(ParseTreeNode child) {
        children.add(child);
    }

    /**
     * 以树形缩进格式输出语法分析树，便于调试文法展开结果。
     */
    public String toPrettyString() {
        StringBuilder builder = new StringBuilder();
        appendPretty(builder, "", true);
        return builder.toString();
    }

    /**
     * 递归地构建树形缩进格式的字符串表示。
     *
     * @param builder 用于构建字符串的 StringBuilder
     * @param prefix  当前节点的前缀字符串，用于表示树的层级关系
     * @param last    当前节点是否是其父节点的最后一个子节点，决定了前缀的显示方式
     */
    private void appendPretty(StringBuilder builder, String prefix, boolean last) {
        builder.append(prefix).append(last ? "└── " : "├── ").append(symbol);
        if (token != null) {
            builder.append(" : ").append(token.lexeme());
        }
        builder.append(System.lineSeparator());

        for (int i = 0; i < children.size(); i++) {
            children.get(i).appendPretty(builder, prefix + (last ? "    " : "│   "), i == children.size() - 1);
        }
    }
}
