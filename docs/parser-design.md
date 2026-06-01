# C++ 编译器第二步：语法分析器设计

## 1. 语法分析器的作用

语法分析器（Parser）的任务，是在词法分析器输出的 Token 序列基础上，按照既定文法检查这些 Token 是否能够组成合法的程序结构，并进一步构造出语法分析树。

如果说词法分析器负责“切词”，那么语法分析器负责“组句”。它关注的不是单个字符，而是 Token 之间的组合关系。例如：

```cpp
int a = 10 + b;
```

词法分析器只会给出一串 Token，而语法分析器需要判断：

- 这是一个变量声明语句
- `10 + b` 是一个表达式
- 这个语句是否符合文法规则

语法分析器的输出通常有两类：

1. **合法性判断**：当前 Token 序列是否符合文法
2. **结构化表示**：生成语法分析树或抽象语法树，供后续语义分析和代码生成使用

---

## 2. 语法分析器的原理

语法分析器的本质，是利用文法规则对 Token 序列进行推导匹配。常见语法分析方法包括：

- 自顶向下分析：递归下降分析、预测分析
- 自底向上分析：LR 分析、移进归约分析

本项目采用**预测分析**作为核心方案。其优点是：

- 实现简单，适合教学和小型编译器
- 结构清晰，便于与文法产生式一一对应
- 可以通过预测分析表实现自动判断，减少回溯

预测分析的核心思想是：

- 从开始符号出发
- 根据当前非终结符和当前输入符号，查表选择唯一的产生式
- 不断展开和匹配，直到分析完成

如果文法满足 **LL(1)** 特性，那么只需要查看一个向前看符号，就可以确定使用哪条产生式。

---

## 3. 本项目采用的文法

为了让语法分析器易于实现，本阶段先设计一个适合教学和基础验证的 C++ 子集文法，而不是完整覆盖全部 C++ 语法。

该子集重点支持：

- 变量声明
- 赋值语句
- 表达式
- 条件语句
- while 循环语句
- 代码块
- return 语句
- 函数定义

### 3.1 文法目标

文法设计要满足以下要求：

1. 尽量避免左递归
2. 尽量消除二义性
3. 方便构造 LL(1) 预测分析表
4. 为后续扩展留出接口

### 3.2 起始符号

本项目可以设定起始符号为：

```text
Program
```

它代表整个源程序。

### 3.3 语法类别

在这个子集文法中，主要类别包括：

- 程序结构
- 声明语句
- 语句序列
- 表达式
- 条件表达式
- 基本项

---

## 4. 文法产生式集设计

下面给出一个适合本项目的简化 LL(1) 文法产生式集。为方便描述，使用 `ε` 表示空串。

### 4.1 程序结构

```text
Program -> DeclList EOF
DeclList -> Decl DeclList | ε
```

程序由若干声明组成，最后以 `EOF` 结束。

### 4.2 声明

```text
Decl -> VarDecl | FuncDecl | ClassDecl
```

这里先预留三种主要声明类型：

- 变量声明
- 函数声明
- 类声明

### 4.3 变量声明

```text
VarDecl -> Type IDENTIFIER VarDeclTail SEMICOLON
VarDeclTail -> ASSIGN Expr | ε
Type -> INT | CHAR | FLOAT | DOUBLE | BOOL | VOID
```

例如：

```cpp
int a;
float b = 3.14;
```

### 4.4 函数声明

```text
FuncDecl -> Type IDENTIFIER LPAREN ParamListOpt RPAREN Block
ParamListOpt -> ParamList | ε
ParamList -> Param ParamListTail
ParamListTail -> COMMA Param ParamListTail | ε
Param -> Type IDENTIFIER
```

例如：

```cpp
int main() { return 0; }
```

### 4.5 类声明

```text
ClassDecl -> CLASS IDENTIFIER LBRACE MemberList RBRACE SEMICOLON
MemberList -> Member MemberList | ε
Member -> VarDecl | FuncDecl
```

这部分支持类声明外壳和成员变量/成员函数定义，用于教学展示作用域和成员登记；当前文法不支持对象实例化、成员访问或方法调用。

### 4.6 语句块与语句序列

```text
Block -> LBRACE StmtList RBRACE
StmtList -> Stmt StmtList | ε
Stmt -> VarDecl
      | AssignStmt SEMICOLON
      | IfStmt
      | WhileStmt
      | ReturnStmt SEMICOLON
      | Block
```

### 4.7 赋值语句

```text
AssignStmt -> IDENTIFIER ASSIGN Expr
```

### 4.8 条件语句

```text
IfStmt -> IF LPAREN Cond RPAREN Stmt ElsePart
ElsePart -> ELSE Stmt | ε
```

### 4.9 循环语句

```text
WhileStmt -> WHILE LPAREN Cond RPAREN Stmt
```

当前文法只支持 `while` 循环。词法分析器可以识别 `for`、`++`、`--` 和复合赋值 Token，但语法分析器不会接受这些结构。

### 4.10 返回语句

```text
ReturnStmt -> RETURN ReturnExprOpt
ReturnExprOpt -> Expr | ε
```

### 4.11 表达式

为了保证文法简单且可用于预测分析，这里采用分层表达式结构：

```text
Expr -> LogicOrExpr
LogicOrExpr -> LogicAndExpr LogicOrExprTail
LogicOrExprTail -> OR LogicAndExpr LogicOrExprTail | ε
LogicAndExpr -> EqualityExpr LogicAndExprTail
LogicAndExprTail -> AND EqualityExpr LogicAndExprTail | ε
EqualityExpr -> RelExpr EqualityExprTail
EqualityExprTail -> (EQ | NEQ) RelExpr EqualityExprTail | ε
RelExpr -> AddExpr RelExprTail
RelExprTail -> (LT | GT | LE | GE) AddExpr RelExprTail | ε
AddExpr -> MulExpr AddExprTail
AddExprTail -> (PLUS | MINUS) MulExpr AddExprTail | ε
MulExpr -> UnaryExpr MulExprTail
MulExprTail -> (STAR | SLASH | PERCENT) UnaryExpr MulExprTail | ε
UnaryExpr -> (PLUS | MINUS | NOT) UnaryExpr | Primary
Primary -> IDENTIFIER | INT_LITERAL | FLOAT_LITERAL | CHAR_LITERAL | STRING_LITERAL | TRUE | FALSE | LPAREN Expr RPAREN
```

这一分层结构的目的是明确表达式优先级。函数调用表达式、成员访问表达式和独立表达式语句不在当前子集内。

- 逻辑或 `||`
- 逻辑与 `&&`
- 相等比较
- 关系比较
- 加减
- 乘除模
- 一元运算
- 基本项

### 4.12 条件表达式

```text
Cond -> Expr
```

在子集文法中，条件表达式直接复用普通表达式即可。

---

## 5. 文法对应的 FIRST 集与 FOLLOW 集

为了构造 LL(1) 预测分析表，必须先计算每个非终结符的 FIRST 集和 FOLLOW 集。

### 5.1 FIRST 集的含义

FIRST(X) 表示从符号 X 出发，能够推导出的串的第一个终结符集合。

如果 X 可以推出空串 `ε`，那么 `ε` 也属于 FIRST(X)。

### 5.2 FOLLOW 集的含义

FOLLOW(A) 表示在某个推导过程中，所有可能出现在非终结符 A 右侧的终结符集合。

如果 A 是开始符号，那么 `EOF` 一定属于 FOLLOW(A)。

### 5.3 关键非终结符的 FIRST 集示例

以下给出部分关键 FIRST 集的设计思路：

```text
FIRST(Type) = { INT, CHAR, FLOAT, DOUBLE, BOOL, VOID, IDENTIFIER }
FIRST(Primary) = { IDENTIFIER, INT_LITERAL, FLOAT_LITERAL, CHAR_LITERAL, STRING_LITERAL, TRUE, FALSE, LPAREN }
FIRST(UnaryExpr) = { PLUS, MINUS, NOT } ∪ FIRST(Primary)
FIRST(MulExpr) = FIRST(UnaryExpr)
FIRST(AddExpr) = FIRST(MulExpr)
FIRST(RelExpr) = FIRST(AddExpr)
FIRST(EqualityExpr) = FIRST(RelExpr)
FIRST(LogicAndExpr) = FIRST(EqualityExpr)
FIRST(LogicOrExpr) = FIRST(LogicAndExpr)
```

### 5.4 关键非终结符的 FOLLOW 集示例

FOLLOW 集需要结合文法整体进行迭代计算。这里给出部分常见结果：

```text
FOLLOW(Program) = { EOF }
FOLLOW(Decl) = FIRST(Decl) ∪ { EOF }
FOLLOW(Stmt) = { RBRACE, ELSE, EOF }
FOLLOW(Expr) = { SEMICOLON, RPAREN, COMMA, RBRACKET, RBRACE }
FOLLOW(Cond) = { RPAREN }
FOLLOW(Primary) = FIRST(运算符尾部) ∪ FOLLOW(上层表达式)
```

由于表达式层级较多，FOLLOW 集通常通过“循环迭代直到不再变化”的算法求得，而不是手工一次列完。

---

## 6. FIRST 集与 FOLLOW 集的构造实现

### 6.1 基本思路

构造 FIRST 和 FOLLOW 集的常用方法，是对产生式不断迭代，直到所有集合稳定：

1. 初始化所有 FIRST 集为空
2. 根据产生式逐条加入可确定的首符号
3. 当某个符号可以推出 `ε` 时，将后续符号的 FIRST 集并入当前集合
4. 对 FOLLOW 集，依据“后继符号”和“产生式尾部可空”规则不断传播
5. 重复执行直到没有新元素加入

### 6.2 数据结构设计

可以使用如下结构：

```java
Map<NonTerminal, Set<TokenType>> firstSets = new EnumMap<>(NonTerminal.class);
Map<NonTerminal, Set<TokenType>> followSets = new EnumMap<>(NonTerminal.class);
```

如果文法规模更大，也可以把右部统一表示为符号列表：

```java
public record Production(NonTerminal left, List<GrammarSymbol> right) {}
```

其中 `GrammarSymbol` 可再抽象为终结符和非终结符的统一接口。

### 6.3 FIRST 集计算伪代码

```java
changed = true
while (changed) {
    changed = false
    for each production A -> X1 X2 ... Xn:
        add FIRST(X1) \ {ε} to FIRST(A)
        if FIRST(X1) contains ε:
            add FIRST(X2) \ {ε} to FIRST(A)
        ...
        if every Xi can derive ε:
            add ε to FIRST(A)
}
```

### 6.4 FOLLOW 集计算伪代码

```java
initialize FOLLOW(StartSymbol) with EOF
changed = true
while (changed) {
    changed = false
    for each production A -> X1 X2 ... Xn:
        for each Xi that is non-terminal:
            add FIRST(Xi+1 ... Xn) \ {ε} to FOLLOW(Xi)
            if Xi+1 ... Xn can derive ε:
                add FOLLOW(A) to FOLLOW(Xi)
}
```

### 6.5 关键代码片段示意

```java
private void addFirstOfSequence(List<GrammarSymbol> symbols, Set<TokenType> target) {
    boolean nullable = true;
    for (GrammarSymbol symbol : symbols) {
        target.addAll(firstOf(symbol).withoutEpsilon());
        if (!firstOf(symbol).containsEpsilon()) {
            nullable = false;
            break;
        }
    }
    if (nullable) {
        target.add(TokenType.EPSILON);
    }
}
```

这类辅助方法可以帮助文法分析器在构造 FIRST/FOLLOW 时保持逻辑清晰。

---

## 7. 预测分析表的构造实现

### 7.1 预测分析表的作用

预测分析表是 LL(1) 分析的核心。它用于决定：

- 当前栈顶非终结符是什么
- 当前向前看 Token 是什么
- 应该使用哪条产生式进行展开

若表项唯一，则分析可以无回溯进行。

### 7.2 表项构造规则

对每条产生式 `A -> α`：

1. 将 `FIRST(α) - {ε}` 中的每个终结符 t，对应表项 `M[A, t]` 填入该产生式
2. 如果 `α` 可推出 `ε`，则对 `FOLLOW(A)` 中的每个终结符 b，也填入 `M[A, b] = A -> α`

### 7.3 表结构设计

可以使用二维映射保存预测分析表：

```java
Map<NonTerminal, Map<TokenType, Production>> parseTable = new EnumMap<>(NonTerminal.class);
```

如果表中某个位置出现多个产生式，就说明文法不是 LL(1)，需要回头调整文法。

### 7.4 构造流程

```java
for each production A -> α:
    for each terminal t in FIRST(α) except ε:
        table[A][t] = A -> α
    if α can derive ε:
        for each terminal b in FOLLOW(A):
            table[A][b] = A -> α
```

### 7.5 冲突处理

如果 `table[A][t]` 已经有内容，再尝试写入新产生式，就说明出现冲突。通常有两种处理方式：

- 重新改写文法，消除左递归和左公因子
- 放弃 LL(1) 表驱动，改用更强的分析方法

本项目目标是实现一个简单、可演示的编译器，因此应优先通过文法改写来保持 LL(1) 特性。

---

## 8. 语法分析算法设计

本项目采用表驱动预测分析。其基本流程如下：

1. 预先构造 FIRST 集、FOLLOW 集和预测分析表
2. 初始化分析栈，压入 `EOF` 和开始符号 `Program`
3. 从 Token 流中读取当前向前看符号
4. 循环比较栈顶符号与当前输入符号：
   - 若栈顶是终结符且匹配当前 Token，则弹栈并前进输入
   - 若栈顶是非终结符，则查预测分析表并展开对应产生式
   - 若表项为空，则报语法错误
5. 当栈与输入都到达结束符时，分析成功

### 8.1 栈驱动分析伪代码

```java
stack.push(EOF)
stack.push(StartSymbol)
current = firstToken

while (!stack.isEmpty()) {
    top = stack.peek()

    if (top is terminal) {
        if (top matches current) {
            stack.pop()
            current = nextToken()
        } else {
            syntaxError()
        }
    } else {
        production = parseTable[top][current.type()]
        if (production == null) {
            syntaxError()
        }
        stack.pop()
        push production.right in reverse order
    }
}
```

---

## 9. 语法分析树的生成

语法分析树（Parse Tree）用于展示程序结构的层次关系。和纯粹的匹配过程相比，语法树能够保留语法展开的结果，便于后续阶段使用。

### 9.1 语法树节点设计

可以设计为：

```java
public class ParseTreeNode {
    private final String symbol;
    private final List<ParseTreeNode> children = new ArrayList<>();

    public ParseTreeNode(String symbol) {
        this.symbol = symbol;
    }

    public void addChild(ParseTreeNode child) {
        children.add(child);
    }
}
```

如果想更紧密地结合词法结果，也可以让叶子节点直接保存 `Token`。

### 9.2 构造方式

在表驱动分析中，每当根据某条产生式展开非终结符时：

- 创建一个对应的树节点
- 为该节点生成右部符号的子节点
- 将这些子节点与分析栈同步维护

这样，分析完成后，整棵树自然形成。

### 9.3 关键代码思路

```java
ParseTreeNode parent = stackNode.pop();
Production production = parseTable[top][lookahead];

for (GrammarSymbol symbol : production.right()) {
    ParseTreeNode child = new ParseTreeNode(symbol.name());
    parent.addChild(child);
    if (!symbol.isEpsilon()) {
        stack.push(symbol);
        stackNode.push(child);
    }
}
```

这类同步维护“分析栈 + 树栈”的方法，是生成语法树最直接的实现方式。

---

## 10. Java 模块实现建议

语法分析器模块可以按以下方式组织：

- `GrammarSymbol`：文法符号抽象
- `NonTerminal`：非终结符枚举
- `Production`：产生式表示
- `Grammar`：文法定义与产生式集合
- `FirstFollowCalculator`：FIRST/FOLLOW 计算
- `ParseTableBuilder`：预测分析表构造
- `Parser`：预测分析主流程
- `ParseTreeNode`：语法树节点

### 10.1 产生式表示示例

```java
public record Production(NonTerminal left, List<GrammarSymbol> right) {
}
```

### 10.2 语法分析入口示例

```java
public ParseTreeNode parse(List<Token> tokens) {
    // 预测分析主过程
    // 根据 TokenType 驱动产生式展开
    // 同步生成语法树
    return root;
}
```

---

## 11. 设计小结

本阶段语法分析器的核心目标，是在词法分析器输出 Token 序列的基础上，完成：

- 文法合法性检查
- FIRST 集与 FOLLOW 集计算
- 预测分析表构造
- 栈驱动的 LL(1) 语法分析
- 语法分析树生成

当前设计采用的是适合教学和基础编译器实现的 LL(1) 子集文法，优点是：

- 结构清晰
- 规则明确
- 便于实现和调试
- 方便后续逐步扩展到更完整的 C++ 语法

如果需要，下一步可以继续把这份文档进一步整理成更正式的课程设计章节，或者直接开始实现 `parser` 模块的 Java 代码。