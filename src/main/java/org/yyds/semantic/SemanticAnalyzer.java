package org.yyds.semantic;

import org.yyds.ir.IRGenerator;
import org.yyds.lexer.Token;
import org.yyds.lexer.TokenType;
import org.yyds.parser.ParseTreeNode;

import java.util.List;

/**
 * 语义分析器：遍历语法树，完成作用域管理、符号登记、类型检查，并同步生成中间代码。
 */
public class SemanticAnalyzer {
    /** 中间代码生成器。 */
    private final IRGenerator irGenerator = new IRGenerator();
    /** 当前所在作用域，沿父链构成作用域栈。 */
    private Scope currentScope = new Scope(null, "global");
    /** 当前正在分析的函数返回类型。 */
    private TypeKind currentFunctionReturnType = TypeKind.VOID;

    /**
     * 对整棵语法树执行语义分析，并在遍历过程中生成 IR。
     */
    public void analyze(ParseTreeNode root) {
        visit(root);
    }

    /**
     * 获取本次分析过程中生成的 IR 生成器。
     */
    public IRGenerator getIrGenerator() {
        return irGenerator;
    }

    /**
     * 获取全局作用域，用于后续查看符号表结果。
     */
    public Scope getGlobalScope() {
        Scope scope = currentScope;
        while (scope.getParent() != null) {
            scope = scope.getParent();
        }
        return scope;
    }

    /**
     * 根据语法树节点类型分派到对应的语义处理逻辑。
     */
    private void visit(ParseTreeNode node) {
        if (node == null) {
            return;
        }
        String symbol = node.getSymbol();
        switch (symbol) {
            case "PROGRAM" -> visitChildren(node);
            case "DECL_LIST", "MEMBER_LIST", "STMT_LIST", "PARAM_LIST", "PARAM_LIST_TAIL",
                 "LOGIC_OR_EXPR_TAIL", "LOGIC_AND_EXPR_TAIL", "EQUALITY_EXPR_TAIL",
                 "REL_EXPR_TAIL", "ADD_EXPR_TAIL", "MUL_EXPR_TAIL" -> visitChildren(node);
            case "DECL" -> visitDecl(node);
            case "MEMBER" -> visitMember(node);
            case "BLOCK" -> visitBlock(node);
            case "STMT" -> visitStmt(node);
            case "IF_STMT" -> visitIfStmt(node);
            case "WHILE_STMT" -> visitWhileStmt(node);
            case "RETURN_STMT" -> visitReturnStmt(node);
            default -> visitChildren(node);
        }
    }

    /** 递归访问子节点。 */
    private void visitChildren(ParseTreeNode node) {
        for (ParseTreeNode child : node.getChildren()) {
            visit(child);
        }
    }

    /**
     * 处理普通声明节点：区分类、函数和变量三种情况。
     */
    private void visitDecl(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();
        if (children.isEmpty()) {
            return;
        }

        if (isSymbol(children.getFirst(), TokenType.CLASS)) {
            visitClassDecl(children);
            return;
        }

        if (looksLikeFuncDecl(children)) {
            visitFuncDecl(children);
            return;
        }

        if (looksLikeVarDecl(children)) {
            visitVarDecl(children, currentScope);
            return;
        }

        visitChildren(node);
    }

    /**
     * 处理类成员节点：成员函数需要创建嵌套作用域，成员变量复用变量声明逻辑。
     */
    private void visitMember(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();
        if (children.isEmpty()) {
            return;
        }
        if (looksLikeMemberFuncDecl(children)) {
            visitMemberFuncDecl(children);
            return;
        }
        if (looksLikeMemberVarDecl(children)) {
            visitVarDecl(List.of(children.get(0), children.get(1), children.get(2).getChildren().getFirst()), currentScope);
            return;
        }
        visitChildren(node);
    }

    /**
     * 处理类内成员函数声明，并为函数体建立独立作用域。
     */
    private void visitMemberFuncDecl(List<ParseTreeNode> children) {
        TypeKind returnType = parseType(children.get(0));
        String name = tokenLexeme(children.get(1));
        defineSymbol(currentScope, name, returnType, SymbolKind.FUNCTION, token(children.get(1)));

        ParseTreeNode tail = children.get(2);
        Scope savedScope = currentScope;
        currentScope = new Scope(savedScope, name);
        TypeKind savedReturnType = currentFunctionReturnType;
        currentFunctionReturnType = returnType;

        collectParams(tail.getChildren().get(1));
        visit(tail.getChildren().get(3));

        currentFunctionReturnType = savedReturnType;
        currentScope = savedScope;
    }

    /**
     * 处理类声明：先登记类名，再进入类作用域分析成员。
     */
    private void visitClassDecl(List<ParseTreeNode> children) {
        String className = tokenLexeme(children.get(1));
        defineSymbol(currentScope, className, TypeKind.UNKNOWN, SymbolKind.CLASS, token(children.get(1)));
        Scope saved = currentScope;
        currentScope = new Scope(saved, className);
        visit(children.get(3));
        currentScope = saved;
    }

    /**
     * 处理全局或普通函数声明：登记函数符号、分析参数并分析函数体。
     */
    private void visitFuncDecl(List<ParseTreeNode> children) {
        TypeKind returnType = parseType(children.get(0));
        String name = tokenLexeme(children.get(1));
        defineSymbol(currentScope, name, returnType, SymbolKind.FUNCTION, token(children.get(1)));

        Scope savedScope = currentScope;
        currentScope = new Scope(savedScope, name);
        TypeKind savedReturnType = currentFunctionReturnType;
        currentFunctionReturnType = returnType;

        ParseTreeNode paramListOpt = children.get(3);
        collectParams(paramListOpt);
        visit(children.get(5));

        currentFunctionReturnType = savedReturnType;
        currentScope = savedScope;
    }

    /** 处理可选参数列表，空产生式直接跳过。 */
    private void collectParams(ParseTreeNode paramListOpt) {
        if (paramListOpt.getChildren().isEmpty() || isEpsilon(paramListOpt.getChildren().getFirst())) {
            return;
        }
        visitParamList(paramListOpt.getChildren().getFirst());
    }

    /** 递归处理参数列表中的单个参数与尾部递归部分。 */
    private void visitParamList(ParseTreeNode node) {
        if (node.getChildren().isEmpty()) {
            return;
        }
        visitParam(node.getChildren().get(0));
        if (node.getChildren().size() > 1) {
            visit(node.getChildren().get(1));
        }
    }

    /** 将参数登记到当前函数作用域中。 */
    private void visitParam(ParseTreeNode node) {
        TypeKind type = parseType(node.getChildren().get(0));
        String name = tokenLexeme(node.getChildren().get(1));
        defineSymbol(currentScope, name, type, SymbolKind.PARAMETER, token(node.getChildren().get(1)));
    }

    /**
     * 处理变量声明，并在存在初始化表达式时同步生成赋值 IR。
     */
    private void visitVarDecl(List<ParseTreeNode> children, Scope scope) {
        TypeKind type = parseType(children.get(0));
        String name = tokenLexeme(children.get(1));
        defineSymbol(scope, name, type, SymbolKind.VARIABLE, token(children.get(1)));

        ParseTreeNode tail = children.get(2);
        if (!tail.getChildren().isEmpty() && isSymbol(tail.getChildren().get(0), TokenType.ASSIGN)) {
            ExprResult init = evalExpr(tail.getChildren().get(1));
            ensureAssignable(type, init.type(), token(children.get(1)));
            irGenerator.emit("=", init.place(), null, name);
        }
    }

    /**
     * 处理块语句：进入新的局部作用域，离开时恢复外层作用域。
     */
    private void visitBlock(ParseTreeNode node) {
        Scope saved = currentScope;
        currentScope = new Scope(saved, "block");
        if (node.getChildren().size() >= 2) {
            visit(node.getChildren().get(1));
        } else {
            visitChildren(node);
        }
        currentScope = saved;
    }

    /**
     * 处理单条语句，并按首个子节点判断语句类别。
     */
    private void visitStmt(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();
        if (children.isEmpty()) {
            return;
        }
        ParseTreeNode first = children.getFirst();
        if (isTypeNode(first)) {
            visitVarDecl(children, currentScope);
            return;
        }
        if (isSymbol(first, TokenType.IDENTIFIER)) {
            visitAssignStmt(children.get(1), first);
            return;
        }
        if (isSymbol(first, TokenType.IF)) {
            visitIfStmt(first.getChildren().isEmpty() ? node : first);
            return;
        }
        if (isSymbol(first, TokenType.WHILE)) {
            visitWhileStmt(first.getChildren().isEmpty() ? node : first);
            return;
        }
        if (isSymbol(first, TokenType.RETURN)) {
            visitReturnStmt(first.getChildren().isEmpty() ? node : first);
            return;
        }
        if (isSymbol(first, TokenType.LBRACE)) {
            visitBlock(first.getChildren().isEmpty() ? node : first);
            return;
        }
        visitChildren(node);
    }

    /** 处理赋值语句：先解析右值，再做类型检查并生成赋值 IR。 */
    private void visitAssignStmt(ParseTreeNode tail, ParseTreeNode identifier) {
        String name = tokenLexeme(identifier);
        SymbolInfo symbol = resolveSymbol(name, token(identifier));
        ExprResult right = evalExpr(tail.getChildren().get(1));
        ensureAssignable(symbol.type(), right.type(), token(identifier));
        irGenerator.emit("=", right.place(), null, name);
    }

    /**
     * 处理 if-else：生成条件跳转、false 分支标签和结束标签。
     */
    private void visitIfStmt(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();
        ExprResult cond = evalExpr(children.get(2));
        ensureBooleanLike(cond.type(), token(children.get(2)));
        String falseLabel = irGenerator.newLabel();
        String endLabel = irGenerator.newLabel();
        irGenerator.emit("jz", cond.place(), null, falseLabel);
        visit(children.get(4));
        irGenerator.emit("goto", null, null, endLabel);
        irGenerator.emit("label", null, null, falseLabel);
        visit(children.get(5));
        irGenerator.emit("label", null, null, endLabel);
    }

    /**
     * 处理 while：生成循环入口、条件判断、循环体和回跳标签。
     */
    private void visitWhileStmt(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();
        String beginLabel = irGenerator.newLabel();
        String endLabel = irGenerator.newLabel();
        irGenerator.emit("label", null, null, beginLabel);
        ExprResult cond = evalExpr(children.get(2));
        ensureBooleanLike(cond.type(), token(children.get(2)));
        irGenerator.emit("jz", cond.place(), null, endLabel);
        visit(children.get(4));
        irGenerator.emit("goto", null, null, beginLabel);
        irGenerator.emit("label", null, null, endLabel);
    }

    /**
     * 处理 return：校验返回值是否与当前函数返回类型一致，并生成 return IR。
     */
    private void visitReturnStmt(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();
        if (children.size() == 1 || isEpsilon(children.get(1))) {
            if (currentFunctionReturnType != TypeKind.VOID) {
                throw error("非 void 函数必须返回值", token(children.getFirst()));
            }
            irGenerator.emit("return", null, null, null);
            return;
        }
        ExprResult value = evalExpr(children.get(1));
        if (!currentFunctionReturnType.canAssignFrom(value.type())) {
            throw error("返回值类型不匹配，期望 " + currentFunctionReturnType + "，实际 " + value.type(), token(children.getFirst()));
        }
        irGenerator.emit("return", value.place(), null, null);
    }

    private ExprResult evalExpr(ParseTreeNode node) {
        if (node == null) {
            return new ExprResult(null, TypeKind.UNKNOWN);
        }
        String symbol = node.getSymbol();
        return switch (symbol) {
            case "EXPR" -> evalExpr(node.getChildren().getFirst());
            case "LOGIC_OR_EXPR" -> evalLogicOr(node);
            case "LOGIC_AND_EXPR" -> evalLogicAnd(node);
            case "EQUALITY_EXPR" -> evalEquality(node);
            case "REL_EXPR" -> evalRel(node);
            case "ADD_EXPR" -> evalAdd(node);
            case "MUL_EXPR" -> evalMul(node);
            case "UNARY_EXPR" -> evalUnary(node);
            case "PRIMARY" -> evalPrimary(node);
            default -> {
                if (node.getChildren().isEmpty()) {
                    yield new ExprResult(null, TypeKind.UNKNOWN);
                }
                yield evalExpr(node.getChildren().getFirst());
            }
        };
    }

    private ExprResult evalLogicOr(ParseTreeNode node) {
        if (node.getChildren().size() < 2) {
            return evalExpr(node.getChildren().getFirst());
        }
        ExprResult left = evalExpr(node.getChildren().getFirst());
        TypeKind resultType = left.type();
        String place = left.place();
        ParseTreeNode tail = node.getChildren().get(1);
        for (int i = 0; i + 1 < tail.getChildren().size(); i += 2) {
            ExprResult right = evalExpr(tail.getChildren().get(i + 1));
            ensureBooleanLike(left.type(), tokenFromNode(node));
            ensureBooleanLike(right.type(), tokenFromNode(node));
            String temp = irGenerator.newTemp();
            irGenerator.emit("||", place, right.place(), temp);
            place = temp;
            resultType = TypeKind.BOOL;
            left = new ExprResult(place, resultType);
        }
        return new ExprResult(place, resultType);
    }

    private ExprResult evalLogicAnd(ParseTreeNode node) {
        if (node.getChildren().size() < 2) {
            return evalExpr(node.getChildren().getFirst());
        }
        ExprResult left = evalExpr(node.getChildren().getFirst());
        TypeKind resultType = left.type();
        String place = left.place();
        ParseTreeNode tail = node.getChildren().get(1);
        for (int i = 0; i + 1 < tail.getChildren().size(); i += 2) {
            ExprResult right = evalExpr(tail.getChildren().get(i + 1));
            ensureBooleanLike(left.type(), tokenFromNode(node));
            ensureBooleanLike(right.type(), tokenFromNode(node));
            String temp = irGenerator.newTemp();
            irGenerator.emit("&&", place, right.place(), temp);
            place = temp;
            resultType = TypeKind.BOOL;
            left = new ExprResult(place, resultType);
        }
        return new ExprResult(place, resultType);
    }

    private ExprResult evalEquality(ParseTreeNode node) {
        if (node.getChildren().size() < 2) {
            return evalExpr(node.getChildren().getFirst());
        }
        ExprResult left = evalExpr(node.getChildren().getFirst());
        String place = left.place();
        boolean hasOperator = false;
        ParseTreeNode tail = node.getChildren().get(1);
        for (int i = 0; i + 1 < tail.getChildren().size(); i += 2) {
            if (isEpsilon(tail.getChildren().get(i))) {
                break;
            }
            hasOperator = true;
            TokenType op = tokenType(tail.getChildren().get(i));
            ExprResult right = evalExpr(tail.getChildren().get(i + 1));
            String temp = irGenerator.newTemp();
            irGenerator.emit(op == TokenType.EQ ? "==" : "!=", place, right.place(), temp);
            place = temp;
        }
        return new ExprResult(place, hasOperator ? TypeKind.BOOL : left.type());
    }

    private ExprResult evalRel(ParseTreeNode node) {
        if (node.getChildren().size() < 2) {
            return evalExpr(node.getChildren().getFirst());
        }
        ExprResult left = evalExpr(node.getChildren().getFirst());
        String place = left.place();
        boolean hasOperator = false;
        ParseTreeNode tail = node.getChildren().get(1);
        for (int i = 0; i + 1 < tail.getChildren().size(); i += 2) {
            if (isEpsilon(tail.getChildren().get(i))) {
                break;
            }
            hasOperator = true;
            TokenType op = tokenType(tail.getChildren().get(i));
            if (op == null) {
                throw error("未知关系运算符", token(tail.getChildren().get(i)));
            }
            ExprResult right = evalExpr(tail.getChildren().get(i + 1));
            String temp = irGenerator.newTemp();
            irGenerator.emit(op.name(), place, right.place(), temp);
            place = temp;
        }
        return new ExprResult(place, hasOperator ? TypeKind.BOOL : left.type());
    }

    private ExprResult evalAdd(ParseTreeNode node) {
        if (node.getChildren().size() < 2) {
            return evalExpr(node.getChildren().getFirst());
        }
        ExprResult left = evalExpr(node.getChildren().getFirst());
        String place = left.place();
        TypeKind resultType = left.type();
        ParseTreeNode tail = node.getChildren().get(1);
        for (int i = 0; i + 1 < tail.getChildren().size(); i += 2) {
            TokenType op = tokenType(tail.getChildren().get(i));
            ExprResult right = evalExpr(tail.getChildren().get(i + 1));
            resultType = widerNumeric(left.type(), right.type());
            String temp = irGenerator.newTemp();
            irGenerator.emit(op == TokenType.PLUS ? "+" : "-", place, right.place(), temp);
            place = temp;
            left = new ExprResult(place, resultType);
        }
        return new ExprResult(place, resultType);
    }

    private ExprResult evalMul(ParseTreeNode node) {
        if (node.getChildren().size() < 2) {
            return evalExpr(node.getChildren().getFirst());
        }
        ExprResult left = evalExpr(node.getChildren().getFirst());
        String place = left.place();
        TypeKind resultType = left.type();
        ParseTreeNode tail = node.getChildren().get(1);
        for (int i = 0; i + 1 < tail.getChildren().size(); i += 2) {
            TokenType op = tokenType(tail.getChildren().get(i));
            ExprResult right = evalExpr(tail.getChildren().get(i + 1));
            resultType = widerNumeric(left.type(), right.type());
            String temp = irGenerator.newTemp();
            irGenerator.emit(op == TokenType.STAR ? "*" : op == TokenType.SLASH ? "/" : "%", place, right.place(), temp);
            place = temp;
            left = new ExprResult(place, resultType);
        }
        return new ExprResult(place, resultType);
    }

    private ExprResult evalUnary(ParseTreeNode node) {
        if (node.getChildren().size() == 1) {
            return evalExpr(node.getChildren().getFirst());
        }
        TokenType op = tokenType(node.getChildren().get(0));
        ExprResult operand = evalExpr(node.getChildren().get(1));
        String temp = irGenerator.newTemp();
        irGenerator.emit(op == TokenType.MINUS ? "uminus" : op == TokenType.NOT ? "!" : "+", operand.place(), null, temp);
        return new ExprResult(temp, op == TokenType.NOT ? TypeKind.BOOL : operand.type());
    }

    private ExprResult evalPrimary(ParseTreeNode node) {
        ParseTreeNode child = node.getChildren().getFirst();
        TokenType type = tokenType(child);
        Token token = token(child);
        if (type == null) {
            return new ExprResult(token.lexeme(), TypeKind.UNKNOWN);
        }
        return switch (type) {
            case IDENTIFIER -> {
                SymbolInfo symbol = resolveSymbol(token.lexeme(), token);
                yield new ExprResult(token.lexeme(), symbol.type());
            }
            case INT_LITERAL -> new ExprResult(token.lexeme(), TypeKind.INT);
            case FLOAT_LITERAL -> new ExprResult(token.lexeme(), TypeKind.FLOAT);
            case CHAR_LITERAL -> new ExprResult(token.lexeme(), TypeKind.CHAR);
            case STRING_LITERAL -> new ExprResult(token.lexeme(), TypeKind.STRING);
            case TRUE, FALSE -> new ExprResult(token.lexeme(), TypeKind.BOOL);
            case LPAREN -> evalExpr(child.getChildren().getFirst());
            default -> new ExprResult(token.lexeme(), TypeKind.UNKNOWN);
        };
    }

    private TypeKind parseType(ParseTreeNode node) {
        ParseTreeNode first = node.getChildren().isEmpty() ? node : node.getChildren().getFirst();
        TokenType firstType = tokenType(first);
        if (first.getChildren().isEmpty()) {
            return firstType == null ? TypeKind.UNKNOWN : mapTokenToType(firstType);
        }
        if (firstType == null) {
            return TypeKind.UNKNOWN;
        }
        if (firstType == TokenType.IDENTIFIER) {
            return TypeKind.UNKNOWN;
        }
        return mapTokenToType(firstType);
    }

    private TypeKind mapTokenToType(TokenType tokenType) {
        return switch (tokenType) {
            case INT -> TypeKind.INT;
            case FLOAT, DOUBLE -> TypeKind.FLOAT;
            case CHAR -> TypeKind.CHAR;
            case BOOL -> TypeKind.BOOL;
            case VOID -> TypeKind.VOID;
            default -> TypeKind.UNKNOWN;
        };
    }

    private TypeKind widerNumeric(TypeKind left, TypeKind right) {
        if (left == TypeKind.FLOAT || right == TypeKind.FLOAT) {
            return TypeKind.FLOAT;
        }
        if (left.isNumeric() && right.isNumeric()) {
            return TypeKind.INT;
        }
        return TypeKind.UNKNOWN;
    }

    private void ensureAssignable(TypeKind target, TypeKind source, Token token) {
        if (!target.canAssignFrom(source)) {
            throw error("类型不兼容，期望 " + target + "，实际 " + source, token);
        }
    }

    private void ensureBooleanLike(TypeKind type, Token token) {
        if (type != TypeKind.BOOL && !type.isNumeric()) {
            throw error("条件表达式应为布尔或数值类型，实际为 " + type, token);
        }
    }

    private SymbolInfo resolveSymbol(String name, Token token) {
        SymbolInfo symbol = currentScope.resolve(name);
        if (symbol == null) {
            throw error("变量未声明：" + name, token);
        }
        return symbol;
    }

    private void defineSymbol(Scope scope, String name, TypeKind type, SymbolKind kind, Token token) {
        boolean ok = scope.define(new SymbolInfo(name, type, kind, token.line(), token.column()));
        if (!ok) {
            throw error("重复定义标识符：" + name, token);
        }
    }

    private boolean looksLikeFuncDecl(List<ParseTreeNode> children) {
        return children.size() >= 4 && isTypeNode(children.get(0)) && isSymbol(children.get(1), TokenType.IDENTIFIER) && isSymbol(children.get(2), TokenType.LPAREN);
    }

    private boolean looksLikeVarDecl(List<ParseTreeNode> children) {
        return children.size() >= 3 && isTypeNode(children.get(0)) && isSymbol(children.get(1), TokenType.IDENTIFIER);
    }

    private boolean looksLikeMemberFuncDecl(List<ParseTreeNode> children) {
        return children.size() >= 3
                && isTypeNode(children.get(0))
                && isSymbol(children.get(1), TokenType.IDENTIFIER)
                && "DECL_TAIL".equals(children.get(2).getSymbol())
                && !children.get(2).getChildren().isEmpty()
                && isSymbol(children.get(2).getChildren().getFirst(), TokenType.LPAREN);
    }

    private boolean looksLikeMemberVarDecl(List<ParseTreeNode> children) {
        return children.size() >= 3
                && isTypeNode(children.get(0))
                && isSymbol(children.get(1), TokenType.IDENTIFIER)
                && "DECL_TAIL".equals(children.get(2).getSymbol())
                && !children.get(2).getChildren().isEmpty()
                && "VAR_DECL_TAIL".equals(children.get(2).getChildren().getFirst().getSymbol());
    }

    private boolean isTypeNode(ParseTreeNode node) {
        if (node == null || node.getChildren().isEmpty()) {
            return false;
        }
        TokenType firstType = tokenType(node.getChildren().getFirst());
        return firstType != null && mapTokenToType(firstType) != TypeKind.UNKNOWN;
    }

    private boolean isSymbol(ParseTreeNode node, TokenType type) {
        return node != null && tokenType(node) == type;
    }

    private boolean isEpsilon(ParseTreeNode node) {
        return node != null && "ε".equals(node.getSymbol());
    }

    private Token token(ParseTreeNode node) {
        if (node == null) {
            return new Token(TokenType.UNKNOWN, "", 1, 1);
        }
        if (node.getToken() != null) {
            return node.getToken();
        }
        if (!node.getChildren().isEmpty()) {
            for (ParseTreeNode child : node.getChildren()) {
                Token token = token(child);
                if (token.type() != TokenType.UNKNOWN) {
                    return token;
                }
            }
        }
        return new Token(TokenType.UNKNOWN, node.getSymbol(), 1, 1);
    }

    private TokenType tokenType(ParseTreeNode node) {
        Token token = node == null ? null : node.getToken();
        if (token != null) {
            return token.type();
        }
        if (node != null && node.getChildren().size() == 1 && node.getChildren().getFirst().getToken() != null) {
            return node.getChildren().getFirst().getToken().type();
        }
        return null;
    }

    private String tokenLexeme(ParseTreeNode node) {
        Token token = token(node);
        return token.lexeme();
    }

    private Token tokenFromNode(ParseTreeNode node) {
        return token(node);
    }

    private SemanticException error(String message, Token token) {
        return new SemanticException(message + "，位置：第 " + token.line() + " 行，第 " + token.column() + " 列");
    }
}
