package org.yyds.lexer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 符号表类，负责记录标识符和常量的信息
 */
public class SymbolTable {
    /**
     * 标识符表
     */
    private final Map<String, Symbol> identifiers = new LinkedHashMap<>();
    /**
     * 常量表
     */
    private final Map<String, Symbol> constants = new LinkedHashMap<>();

    /**
     * 记录标识符信息，如果已经存在则增加出现次数，否则创建新的符号表项
     *
     * @param lexeme 标识符的词素
     * @param line   标识符首次出现的行号
     * @param column 标识符首次出现的列号
     */
    public void recordIdentifier(String lexeme, int line, int column) {
        record(identifiers, lexeme, TokenType.IDENTIFIER, line, column);
    }

    /**
     * 记录常量信息，如果已经存在则增加出现次数，否则创建新的符号表项
     *
     * @param lexeme 常量的词素
     * @param type   常量的类型（整数、浮点数、字符串等）
     * @param line   常量首次出现的行号
     * @param column 常量首次出现的列号
     */
    public void recordConstant(String lexeme, TokenType type, int line, int column) {
        record(constants, lexeme, type, line, column);
    }

    /**
     * 获取标识符表的不可修改视图，外部只能读取但不能修改符号表
     *
     * @return 标识符表的不可修改视图
     */
    public Map<String, Symbol> getIdentifiers() {
        return Collections.unmodifiableMap(identifiers);
    }

    /**
     * 获取常量表的不可修改视图，外部只能读取但不能修改符号表
     *
     * @return 常量表的不可修改视图
     */
    public Map<String, Symbol> getConstants() {
        return Collections.unmodifiableMap(constants);
    }

    /**
     * 记录符号信息的通用方法，根据词素和类型在指定的表中查找，如果不存在则创建新的符号表项，否则增加出现次数
     *
     * @param table  要记录的符号表（标识符表或常量表）
     * @param lexeme 词素
     * @param type   词素类型
     * @param line   词素首次出现的行号
     * @param column 词素首次出现的列号
     */
    private void record(Map<String, Symbol> table, String lexeme, TokenType type, int line, int column) {
        Symbol symbol = table.get(lexeme);
        if (symbol == null) {
            table.put(lexeme, new Symbol(lexeme, type, line, column));
        } else {
            symbol.increaseOccurrences();
        }
    }
}
