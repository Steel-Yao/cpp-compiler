package org.yyds.gui;

import org.yyds.codegen.CodeGenerator;
import org.yyds.codegen.TargetInstruction;
import org.yyds.ir.Quadruple;
import org.yyds.lexer.Lexer;
import org.yyds.lexer.Symbol;
import org.yyds.lexer.Token;
import org.yyds.optimizer.Optimizer;
import org.yyds.parser.ParseTreeNode;
import org.yyds.parser.Parser;
import org.yyds.semantic.SemanticAnalyzer;

import java.util.List;

/**
 * Swing 界面复用的编译流水线门面，按词法、语法、语义、优化和目标代码生成顺序执行。
 */
public class CompilerPipeline {
    /**
     * 编译一段 C++ 子集源码，并收集 GUI 各标签页需要展示的中间结果。
     *
     * @param source 待编译源码，允许为空字符串
     * @return 各编译阶段的格式化结果和耗时
     */
    public CompilationResult compile(String source) {
        long startNanos = System.nanoTime();

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        List<Symbol> identifiers = lexer.getSymbolTable().getIdentifiers().values().stream().toList();
        List<Symbol> constants = lexer.getSymbolTable().getConstants().values().stream().toList();

        Parser parser = new Parser();
        ParseTreeNode parseTree = parser.parse(tokens);

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(parseTree);
        List<Quadruple> quadruples = semanticAnalyzer.getIrGenerator().getQuadruples();

        Optimizer optimizer = new Optimizer();
        List<Quadruple> optimizedQuadruples = optimizer.optimize(quadruples);

        CodeGenerator codeGenerator = new CodeGenerator();
        List<TargetInstruction> targetCode = codeGenerator.generate(optimizedQuadruples);

        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        return new CompilationResult(
                tokens,
                identifiers,
                constants,
                parseTree.toPrettyString(),
                quadruples,
                optimizedQuadruples,
                targetCode,
                elapsedMillis
        );
    }
}
