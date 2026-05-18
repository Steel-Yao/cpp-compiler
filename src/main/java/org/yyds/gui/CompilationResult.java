package org.yyds.gui;

import org.yyds.codegen.TargetInstruction;
import org.yyds.ir.Quadruple;
import org.yyds.lexer.Symbol;
import org.yyds.lexer.Token;

import java.util.List;

public record CompilationResult(
        List<Token> tokens,
        List<Symbol> identifiers,
        List<Symbol> constants,
        String parseTree,
        List<Quadruple> quadruples,
        List<Quadruple> optimizedQuadruples,
        List<TargetInstruction> targetCode,
        long elapsedMillis
) {
    public CompilationResult {
        tokens = List.copyOf(tokens);
        identifiers = List.copyOf(identifiers);
        constants = List.copyOf(constants);
        quadruples = List.copyOf(quadruples);
        optimizedQuadruples = List.copyOf(optimizedQuadruples);
        targetCode = List.copyOf(targetCode);
    }
}
