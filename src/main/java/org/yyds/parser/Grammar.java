package org.yyds.parser;

import org.yyds.lexer.TokenType;

import java.util.*;

import static org.yyds.parser.GrammarSymbol.*;

/**
 * 集中保存语法分析器采用的 C++ 子集文法产生式。
 */
public class Grammar {
    /**
     * 文法的所有产生式，按照定义顺序排列。
     */
    private final List<Production> productions = new ArrayList<>();
    /**
     * 按照左部非终结符分类的产生式列表，方便查询某个非终结符的所有产生式。
     */
    private final Map<NonTerminal, List<Production>> productionsByLeft = new EnumMap<>(NonTerminal.class);

    public Grammar() {
        defineGrammar();
    }

    /**
     * 定义文法的开始符号，即程序的根节点。
     *
     * @return 程序的开始符号，通常是一个非终结符，如 {@code PROGRAM}。
     */
    public NonTerminal startSymbol() {
        return NonTerminal.PROGRAM;
    }

    /**
     * 获取文法的所有产生式列表，按照定义顺序排列。
     *
     * @return 不可修改的产生式列表，包含文法的所有产生式。
     */
    public List<Production> productions() {
        return Collections.unmodifiableList(productions);
    }

    /**
     * 获取指定非终结符的所有产生式列表。
     *
     * @param nonTerminal 需要查询的非终结符。
     * @return 不可修改的产生式列表，包含所有以指定非终结符为左部的产生式。
     * 如果没有任何产生式以该非终结符为左部，则返回一个空列表。
     */
    public List<Production> productionsOf(NonTerminal nonTerminal) {
        return productionsByLeft.getOrDefault(nonTerminal, List.of());
    }

    /**
     * 定义文法的产生式。每条产生式由一个左部非终结符和一个右部符号序列组成。
     */
    private void defineGrammar() {
        // 程序 -> 声明列表 EOF
        // 表示一个程序由一系列声明组成，最后以文件结束符结束。
        add(NonTerminal.PROGRAM, nt(NonTerminal.DECL_LIST), t(TokenType.EOF));
        // 声明列表 -> 声明 声明列表 | ε
        // 表示程序可以包含多个声明，也可以没有任何声明。
        add(NonTerminal.DECL_LIST, nt(NonTerminal.DECL), nt(NonTerminal.DECL_LIST));
        add(NonTerminal.DECL_LIST, EPSILON);

        // 声明 -> 类 界符标符 { 成员列表 } ; | 类型 标识符 声明尾部
        // 表示声明可以是一个类定义，也可以是一个变量或函数声明。
        add(NonTerminal.DECL, t(TokenType.CLASS), t(TokenType.IDENTIFIER), t(TokenType.LBRACE), nt(NonTerminal.MEMBER_LIST), t(TokenType.RBRACE), t(TokenType.SEMICOLON));
        add(NonTerminal.DECL, nt(NonTerminal.TYPE), t(TokenType.IDENTIFIER), nt(NonTerminal.DECL_TAIL));
        add(NonTerminal.DECL_TAIL, t(TokenType.LPAREN), nt(NonTerminal.PARAM_LIST_OPT), t(TokenType.RPAREN), nt(NonTerminal.BLOCK));
        add(NonTerminal.DECL_TAIL, nt(NonTerminal.VAR_DECL_TAIL), t(TokenType.SEMICOLON));

        // 类型 -> int | char | float | double | bool | void
        // 表示文法中的类型非终结符可以是这些基本类型之一。
        addTypes(NonTerminal.TYPE);

        // 变量声明尾部 -> = 表达式 | ε
        // 表示变量声明可以有一个可选的初始化表达式。
        add(NonTerminal.VAR_DECL_TAIL, t(TokenType.ASSIGN), nt(NonTerminal.EXPR));
        add(NonTerminal.VAR_DECL_TAIL, EPSILON);

        // 参数列表可选 -> 参数列表 | ε
        // 表示函数参数列表可以有多个参数，也可以没有任何参数。
        add(NonTerminal.PARAM_LIST_OPT, nt(NonTerminal.PARAM_LIST));
        add(NonTerminal.PARAM_LIST_OPT, EPSILON);
        add(NonTerminal.PARAM_LIST, nt(NonTerminal.PARAM), nt(NonTerminal.PARAM_LIST_TAIL));
        add(NonTerminal.PARAM_LIST_TAIL, t(TokenType.COMMA), nt(NonTerminal.PARAM), nt(NonTerminal.PARAM_LIST_TAIL));
        add(NonTerminal.PARAM_LIST_TAIL, EPSILON);
        add(NonTerminal.PARAM, nt(NonTerminal.TYPE), t(TokenType.IDENTIFIER));

        // 成员列表 -> 访问说明符_opt 成员 成员列表 | ε
        // 表示类的成员可以有一个可选的访问说明符，后面跟一个成员定义，
        // 成员定义后面可以继续有更多成员，也可以没有任何成员。
        add(NonTerminal.MEMBER_LIST, nt(NonTerminal.ACCESS_SPEC_OPT), nt(NonTerminal.MEMBER), nt(NonTerminal.MEMBER_LIST));
        add(NonTerminal.MEMBER_LIST, EPSILON);
        add(NonTerminal.ACCESS_SPEC_OPT, t(TokenType.PUBLIC), t(TokenType.COLON));
        add(NonTerminal.ACCESS_SPEC_OPT, t(TokenType.PRIVATE), t(TokenType.COLON));
        add(NonTerminal.ACCESS_SPEC_OPT, t(TokenType.PROTECTED), t(TokenType.COLON));
        add(NonTerminal.ACCESS_SPEC_OPT, EPSILON);
        add(NonTerminal.MEMBER, nt(NonTerminal.TYPE), t(TokenType.IDENTIFIER), nt(NonTerminal.DECL_TAIL));

        // 语句 -> 类型 标识符 变量声明尾部 ; | 标识符更新 ; | if 语句 | while 语句 | for 语句 | return 语句 ; | 块
        // 标识符更新支持普通赋值、复合赋值和自增自减。
        add(NonTerminal.BLOCK, t(TokenType.LBRACE), nt(NonTerminal.STMT_LIST), t(TokenType.RBRACE));
        add(NonTerminal.STMT_LIST, nt(NonTerminal.STMT), nt(NonTerminal.STMT_LIST));
        add(NonTerminal.STMT_LIST, EPSILON);

        add(NonTerminal.STMT, nt(NonTerminal.TYPE), t(TokenType.IDENTIFIER), nt(NonTerminal.VAR_DECL_TAIL), t(TokenType.SEMICOLON));
        add(NonTerminal.STMT, t(TokenType.IDENTIFIER), nt(NonTerminal.IDENTIFIER_STMT_TAIL), t(TokenType.SEMICOLON));
        add(NonTerminal.STMT, nt(NonTerminal.IF_STMT));
        add(NonTerminal.STMT, nt(NonTerminal.WHILE_STMT));
        add(NonTerminal.STMT, nt(NonTerminal.FOR_STMT));
        add(NonTerminal.STMT, nt(NonTerminal.RETURN_STMT), t(TokenType.SEMICOLON));
        add(NonTerminal.STMT, nt(NonTerminal.BLOCK));

        add(NonTerminal.IDENTIFIER_STMT_TAIL, t(TokenType.ASSIGN), nt(NonTerminal.EXPR));
        add(NonTerminal.IDENTIFIER_STMT_TAIL, nt(NonTerminal.COMPOUND_ASSIGN_OP), nt(NonTerminal.EXPR));
        add(NonTerminal.IDENTIFIER_STMT_TAIL, t(TokenType.INC));
        add(NonTerminal.IDENTIFIER_STMT_TAIL, t(TokenType.DEC));
        add(NonTerminal.COMPOUND_ASSIGN_OP, t(TokenType.PLUS_ASSIGN));
        add(NonTerminal.COMPOUND_ASSIGN_OP, t(TokenType.MINUS_ASSIGN));
        add(NonTerminal.COMPOUND_ASSIGN_OP, t(TokenType.STAR_ASSIGN));
        add(NonTerminal.COMPOUND_ASSIGN_OP, t(TokenType.SLASH_ASSIGN));
        add(NonTerminal.COMPOUND_ASSIGN_OP, t(TokenType.PERCENT_ASSIGN));

        add(NonTerminal.IF_STMT, t(TokenType.IF), t(TokenType.LPAREN), nt(NonTerminal.EXPR), t(TokenType.RPAREN), nt(NonTerminal.STMT), nt(NonTerminal.ELSE_PART));
        add(NonTerminal.ELSE_PART, t(TokenType.ELSE), nt(NonTerminal.STMT));
        add(NonTerminal.ELSE_PART, EPSILON);

        add(NonTerminal.WHILE_STMT, t(TokenType.WHILE), t(TokenType.LPAREN), nt(NonTerminal.EXPR), t(TokenType.RPAREN), nt(NonTerminal.STMT));
        add(NonTerminal.FOR_STMT, t(TokenType.FOR), t(TokenType.LPAREN), nt(NonTerminal.FOR_INIT_OPT), t(TokenType.SEMICOLON), nt(NonTerminal.FOR_COND_OPT), t(TokenType.SEMICOLON), nt(NonTerminal.FOR_STEP_OPT), t(TokenType.RPAREN), nt(NonTerminal.STMT));
        add(NonTerminal.FOR_INIT_OPT, nt(NonTerminal.TYPE), t(TokenType.IDENTIFIER), nt(NonTerminal.VAR_DECL_TAIL));
        add(NonTerminal.FOR_INIT_OPT, t(TokenType.IDENTIFIER), nt(NonTerminal.IDENTIFIER_STMT_TAIL));
        add(NonTerminal.FOR_INIT_OPT, EPSILON);
        add(NonTerminal.FOR_COND_OPT, nt(NonTerminal.EXPR));
        add(NonTerminal.FOR_COND_OPT, EPSILON);
        add(NonTerminal.FOR_STEP_OPT, t(TokenType.IDENTIFIER), nt(NonTerminal.IDENTIFIER_STMT_TAIL));
        add(NonTerminal.FOR_STEP_OPT, EPSILON);
        add(NonTerminal.RETURN_STMT, t(TokenType.RETURN), nt(NonTerminal.RETURN_EXPR_OPT));
        add(NonTerminal.RETURN_EXPR_OPT, nt(NonTerminal.EXPR));
        add(NonTerminal.RETURN_EXPR_OPT, EPSILON);

        add(NonTerminal.EXPR, nt(NonTerminal.LOGIC_OR_EXPR));
        add(NonTerminal.LOGIC_OR_EXPR, nt(NonTerminal.LOGIC_AND_EXPR), nt(NonTerminal.LOGIC_OR_EXPR_TAIL));
        add(NonTerminal.LOGIC_OR_EXPR_TAIL, t(TokenType.OR), nt(NonTerminal.LOGIC_AND_EXPR), nt(NonTerminal.LOGIC_OR_EXPR_TAIL));
        add(NonTerminal.LOGIC_OR_EXPR_TAIL, EPSILON);

        add(NonTerminal.LOGIC_AND_EXPR, nt(NonTerminal.EQUALITY_EXPR), nt(NonTerminal.LOGIC_AND_EXPR_TAIL));
        add(NonTerminal.LOGIC_AND_EXPR_TAIL, t(TokenType.AND), nt(NonTerminal.EQUALITY_EXPR), nt(NonTerminal.LOGIC_AND_EXPR_TAIL));
        add(NonTerminal.LOGIC_AND_EXPR_TAIL, EPSILON);

        add(NonTerminal.EQUALITY_EXPR, nt(NonTerminal.REL_EXPR), nt(NonTerminal.EQUALITY_EXPR_TAIL));
        add(NonTerminal.EQUALITY_EXPR_TAIL, t(TokenType.EQ), nt(NonTerminal.REL_EXPR), nt(NonTerminal.EQUALITY_EXPR_TAIL));
        add(NonTerminal.EQUALITY_EXPR_TAIL, t(TokenType.NEQ), nt(NonTerminal.REL_EXPR), nt(NonTerminal.EQUALITY_EXPR_TAIL));
        add(NonTerminal.EQUALITY_EXPR_TAIL, EPSILON);

        add(NonTerminal.REL_EXPR, nt(NonTerminal.ADD_EXPR), nt(NonTerminal.REL_EXPR_TAIL));
        add(NonTerminal.REL_EXPR_TAIL, t(TokenType.LT), nt(NonTerminal.ADD_EXPR), nt(NonTerminal.REL_EXPR_TAIL));
        add(NonTerminal.REL_EXPR_TAIL, t(TokenType.GT), nt(NonTerminal.ADD_EXPR), nt(NonTerminal.REL_EXPR_TAIL));
        add(NonTerminal.REL_EXPR_TAIL, t(TokenType.LE), nt(NonTerminal.ADD_EXPR), nt(NonTerminal.REL_EXPR_TAIL));
        add(NonTerminal.REL_EXPR_TAIL, t(TokenType.GE), nt(NonTerminal.ADD_EXPR), nt(NonTerminal.REL_EXPR_TAIL));
        add(NonTerminal.REL_EXPR_TAIL, EPSILON);

        add(NonTerminal.ADD_EXPR, nt(NonTerminal.MUL_EXPR), nt(NonTerminal.ADD_EXPR_TAIL));
        add(NonTerminal.ADD_EXPR_TAIL, t(TokenType.PLUS), nt(NonTerminal.MUL_EXPR), nt(NonTerminal.ADD_EXPR_TAIL));
        add(NonTerminal.ADD_EXPR_TAIL, t(TokenType.MINUS), nt(NonTerminal.MUL_EXPR), nt(NonTerminal.ADD_EXPR_TAIL));
        add(NonTerminal.ADD_EXPR_TAIL, EPSILON);

        add(NonTerminal.MUL_EXPR, nt(NonTerminal.UNARY_EXPR), nt(NonTerminal.MUL_EXPR_TAIL));
        add(NonTerminal.MUL_EXPR_TAIL, t(TokenType.STAR), nt(NonTerminal.UNARY_EXPR), nt(NonTerminal.MUL_EXPR_TAIL));
        add(NonTerminal.MUL_EXPR_TAIL, t(TokenType.SLASH), nt(NonTerminal.UNARY_EXPR), nt(NonTerminal.MUL_EXPR_TAIL));
        add(NonTerminal.MUL_EXPR_TAIL, t(TokenType.PERCENT), nt(NonTerminal.UNARY_EXPR), nt(NonTerminal.MUL_EXPR_TAIL));
        add(NonTerminal.MUL_EXPR_TAIL, EPSILON);

        add(NonTerminal.UNARY_EXPR, t(TokenType.PLUS), nt(NonTerminal.UNARY_EXPR));
        add(NonTerminal.UNARY_EXPR, t(TokenType.MINUS), nt(NonTerminal.UNARY_EXPR));
        add(NonTerminal.UNARY_EXPR, t(TokenType.NOT), nt(NonTerminal.UNARY_EXPR));
        add(NonTerminal.UNARY_EXPR, nt(NonTerminal.PRIMARY));

        add(NonTerminal.PRIMARY, t(TokenType.IDENTIFIER));
        add(NonTerminal.PRIMARY, t(TokenType.INT_LITERAL));
        add(NonTerminal.PRIMARY, t(TokenType.FLOAT_LITERAL));
        add(NonTerminal.PRIMARY, t(TokenType.CHAR_LITERAL));
        add(NonTerminal.PRIMARY, t(TokenType.STRING_LITERAL));
        add(NonTerminal.PRIMARY, t(TokenType.TRUE));
        add(NonTerminal.PRIMARY, t(TokenType.FALSE));
        add(NonTerminal.PRIMARY, t(TokenType.LPAREN), nt(NonTerminal.EXPR), t(TokenType.RPAREN));
    }

    /**
     * 定义文法中所有类型的产生式。对于每个类型非终结符，我们添加了对应的基本类型作为右部符号。
     *
     * @param left 需要添加类型产生式的非终结符，通常是表示类型的非终结符，如 {@code TYPE}。
     */
    private void addTypes(NonTerminal left) {
        add(left, t(TokenType.INT));
        add(left, t(TokenType.CHAR));
        add(left, t(TokenType.FLOAT));
        add(left, t(TokenType.DOUBLE));
        add(left, t(TokenType.BOOL));
        add(left, t(TokenType.VOID));
    }

    /**
     * 辅助方法，简化产生式的添加。每条产生式由一个左部非终结符和一个右部符号序列组成。
     *
     * @param left  产生式的左部非终结符，表示该产生式定义了什么样的结构。
     * @param right 产生式的右部符号序列，表示该结构由哪些符号组成，可以是终结符、非终结符或空串。
     */
    private void add(NonTerminal left, GrammarSymbol... right) {
        Production production = new Production(left, List.of(right));
        productions.add(production);
        productionsByLeft.computeIfAbsent(left, ignored -> new ArrayList<>()).add(production);
    }

    /**
     * 辅助方法，简化终结符的创建。
     *
     * @param terminal 需要创建的终结符类型。
     * @return 包装了指定终结符类型的 GrammarSymbol 实例，表示文法中的一个终结符。
     */
    private GrammarSymbol t(TokenType terminal) {
        return terminal(terminal);
    }

    /**
     * 辅助方法，简化非终结符的创建。
     *
     * @param nonTerminal 需要创建的非终结符类型。
     * @return 包装了指定非终结符类型的 GrammarSymbol 实例，表示文法中的一个非终结符。
     */
    private GrammarSymbol nt(NonTerminal nonTerminal) {
        return nonTerminal(nonTerminal);
    }
}
