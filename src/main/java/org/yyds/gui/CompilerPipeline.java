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

public class CompilerPipeline {
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
