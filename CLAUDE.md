# CLAUDE.md

本文档用于指导 Claude Code（claude.ai/code）在本仓库中工作。

## 项目概览

这是一个教学导向的 C++ 子集编译器，使用 Java 21 编写。当前实现覆盖从前端到简化后端的完整演示流水线：

1. `Lexer` 将源代码转换为 Token，并记录词法标识符/常量。
2. `Parser` 使用表驱动的 LL(1) 预测分析器构建语法树。
3. `SemanticAnalyzer` 遍历语法树，管理作用域，执行基础类型检查，并生成 IR。
4. `IRGenerator` 保存三地址码风格的四元式。
5. `Optimizer` 对四元式执行基础中间代码优化。
6. `CodeGenerator` 将优化后的四元式转换为教学型伪汇编目标代码。
7. `Main` 通过一个内置的 C++ 风格示例程序演示命令行完整流程。
8. `CompilerGui` 提供 Swing 可视化界面，用于输入源代码并分区查看各阶段结果。

该编译器是简化版子集，不是完整的 C++ 编译器。设计说明见 `docs/lexer-design.md`、`docs/parser-design.md`、`docs/semantic-ir-design.md`、`docs/optimizer-design.md` 和 `docs/codegen-design.md`。

## 常用命令

这是一个面向 Java 21 的 Maven 项目（`pom.xml`）。在当前观察到的本地环境里没有可用的 Maven，但安装 Maven 后通常使用这些命令：

```bash
mvn compile
mvn test
mvn -Dtest=SomeTest test
mvn clean
```

当前没有 `src/test` 测试文件。使用 Maven 编译后运行演示入口：

```bash
mvn compile
java -cp target/classes org.yyds.Main
```

如果没有 Maven、但安装了 JDK 21，可以直接编译和运行命令行演示：

```bash
javac -d target/classes $(find src/main/java -name '*.java')
java -cp target/classes org.yyds.Main
```

运行 Swing 可视化界面：

```bash
java -cp target/classes org.yyds.gui.CompilerGui
```

## 架构说明

### Lexer

词法分析器位于 `src/main/java/org/yyds/lexer`。`Lexer` 扫描源字符串，输出 `Token`，并在末尾追加 `EOF`。它识别基础的 C++ 风格关键字、标识符、数字/字符串/字符字面量、注释、运算符和分隔符。`SymbolTable` 保存词法阶段发现的标识符和常量，这与语义阶段的作用域表不同。

### Parser

语法分析器位于 `src/main/java/org/yyds/parser`。文法集中定义在 `Grammar` 中，`Production`、`GrammarSymbol` 和 `NonTerminal` 表示产生式与符号。`FirstFollowCalculator` 计算 FIRST/FOLLOW 集，`ParseTableBuilder` 构建 LL(1) 预测分析表，`Parser` 以栈驱动方式进行预测分析并构建 `ParseTreeNode`。

当需要修改可接受语法时，应联动更新相关类型：新增终结符时改 `TokenType`/`Lexer`，新增产生式时改 `NonTerminal` 和 `Grammar`，如果语法树形状变化，再调整语义树遍历逻辑。

### 语义分析与 IR

语义分析位于 `src/main/java/org/yyds/semantic`，IR 位于 `src/main/java/org/yyds/ir`。`SemanticAnalyzer` 递归访问 `ParseTreeNode`，维护父子链接的 `Scope`，为变量/函数/类/参数建立 `SymbolInfo`，通过 `TypeKind` 检查可赋值性，并借助 `IRGenerator` 生成四元式。

IR 以 `Quadruple(op, arg1, arg2, result)` 表示。`IRGenerator` 维护临时变量和标签计数器（如 `t1`、`L1`），并提供不可变的四元式列表。

### Optimizer

优化器位于 `src/main/java/org/yyds/optimizer`。`Optimizer` 以 `List<Quadruple>` 为输入和输出，先按标签与跳转划分 `BasicBlock`，再执行基础中间代码优化，包括常量折叠、常量传播、复制传播、公共子表达式消除、死代码删除和简单跳转清理。`OptimizationPass` 是后续拆分优化 pass 的扩展接口。

### Code Generator

目标代码生成器位于 `src/main/java/org/yyds/codegen`。`CodeGenerator` 读取优化后的四元式，生成教学型伪汇编 `TargetInstruction` 列表。当前采用固定工作寄存器 `R1` 的简单翻译策略，支持赋值、算术/关系/逻辑运算、标签、跳转和 `return`。遇到不支持或结构不完整的四元式时抛出 `CodeGenerationException`。

### GUI

可视化界面位于 `src/main/java/org/yyds/gui`。`CompilerGui` 使用 Java Swing 构建桌面窗口；`CompilerPipeline` 封装 Lexer、Parser、SemanticAnalyzer、Optimizer 和 CodeGenerator 的完整调用链；`CompilationResult` 保存各阶段格式化后的输出文本，供界面标签页展示。

### 演示流程

`src/main/java/org/yyds/Main.java` 是命令行可运行入口。它内置一段源程序，打印 Token、词法标识符/常量表、语法树、原始四元式、优化后四元式以及伪汇编目标代码。

`src/main/java/org/yyds/gui/CompilerGui.java` 是 Swing 可视化入口。它提供源码输入区和结果标签页，调用 `CompilerPipeline` 复用现有编译流水线，并展示 Token 序列、符号表、语法树、四元式、优化后四元式、目标代码和错误信息。当前还没有 CLI 参数处理或源文件读取功能。
