package org.yyds.semantic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个作用域，使用 parent 串联成作用域链。
 */
public class Scope {
    /** 上一级作用域。 */
    private final Scope parent;
    /** 当前作用域名称，便于调试和定位。 */
    private final String name;
    /** 当前作用域内定义的符号，保持插入顺序以便打印。 */
    private final Map<String, SymbolInfo> symbols = new LinkedHashMap<>();

    /**
     * 创建一个新作用域。
     *
     * @param parent 上一级作用域
     * @param name   当前作用域名称
     */
    public Scope(Scope parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    /**
     * 获取父作用域。
     */
    public Scope getParent() {
        return parent;
    }

    /**
     * 获取当前作用域名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 获取当前作用域内的符号表快照。
     */
    public Map<String, SymbolInfo> getSymbols() {
        return Collections.unmodifiableMap(symbols);
    }

    /**
     * 在当前作用域中定义一个新符号。
     *
     * @param symbol 需要登记的符号
     * @return 如果当前作用域中已存在同名符号则返回 `false`
     */
    public boolean define(SymbolInfo symbol) {
        if (symbols.containsKey(symbol.name())) {
            return false;
        }
        symbols.put(symbol.name(), symbol);
        return true;
    }

    /**
     * 按当前作用域优先、向外层回溯查找符号。
     *
     * @param name 符号名
     * @return 找到则返回符号，否则返回 {@code null}
     */
    public SymbolInfo resolve(String name) {
        SymbolInfo symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        }
        return parent == null ? null : parent.resolve(name);
    }
}
