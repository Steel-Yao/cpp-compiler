package org.yyds;

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

public class Main {
    public static void main(String[] args) {
        String source = """
            class Demo {
            	private:
            		int x;
            		double y;
            		char tag;
            	public:
            		int main() {
            			int a = 10;
            			int b = 3;
            			double rate = 2.5;
            			char ch = 'A';
            
            			int folded = 2 + 3 * 4;
            			int copy = folded;
            			int result = copy + a;
            
            			while (result < 30) {
            				result = result + b;
            			}
            
            			if (result >= 30 && b != 0) {
            				return result + 1;
            			}
            
            			return 0;
            		}
            };
            """;

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        System.out.println("Token 序列：");
        for (Token token : tokens) {
            System.out.println(token);
        }

        System.out.println("\n标识符表：");
        for (Symbol symbol : lexer.getSymbolTable().getIdentifiers().values()) {
            System.out.println(symbol);
        }

        System.out.println("\n常量表：");
        for (Symbol symbol : lexer.getSymbolTable().getConstants().values()) {
            System.out.println(symbol);
        }

        Parser parser = new Parser();
        ParseTreeNode parseTree = parser.parse(tokens);
        System.out.println("\n语法分析树：");
        System.out.println(parseTree.toPrettyString());

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(parseTree);
        List<Quadruple> quadruples = semanticAnalyzer.getIrGenerator().getQuadruples();
        System.out.println("\n中间代码四元式：");
        for (Quadruple quadruple : quadruples) {
            System.out.println(quadruple);
        }

        Optimizer optimizer = new Optimizer();
        List<Quadruple> optimizedQuadruples = optimizer.optimize(quadruples);
        System.out.println("\n优化后四元式：");
        for (Quadruple quadruple : optimizedQuadruples) {
            System.out.println(quadruple);
        }

        CodeGenerator codeGenerator = new CodeGenerator();
        List<TargetInstruction> targetCode = codeGenerator.generate(optimizedQuadruples);
        System.out.println("\n目标代码：");
        for (TargetInstruction instruction : targetCode) {
            System.out.println(instruction);
        }
    }
}
