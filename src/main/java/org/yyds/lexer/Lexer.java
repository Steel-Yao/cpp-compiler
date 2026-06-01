package org.yyds.lexer;

import java.util.*;

/**
 * 词法分析器类，负责将源代码字符串转换为 {@link Token} 序列，并记录标识符和常量的信息到符号表中
 */
public class Lexer {
    /**
     * 关键字映射表，预定义了语言中的保留字及其对应的 {@link TokenType}
     */
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("int", TokenType.INT),
            Map.entry("char", TokenType.CHAR),
            Map.entry("float", TokenType.FLOAT),
            Map.entry("double", TokenType.DOUBLE),
            Map.entry("bool", TokenType.BOOL),
            Map.entry("void", TokenType.VOID),
            Map.entry("if", TokenType.IF),
            Map.entry("else", TokenType.ELSE),
            Map.entry("while", TokenType.WHILE),
            Map.entry("for", TokenType.FOR),
            Map.entry("return", TokenType.RETURN),
            Map.entry("class", TokenType.CLASS),
            Map.entry("public", TokenType.PUBLIC),
            Map.entry("private", TokenType.PRIVATE),
            Map.entry("protected", TokenType.PROTECTED),
            Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE)
    );

    /**
     * 操作符映射表，预定义了语言中的运算符及其对应的 {@link TokenType}
     */
    private static final Map<String, TokenType> OPERATORS = Map.ofEntries(
            Map.entry("+", TokenType.PLUS),
            Map.entry("-", TokenType.MINUS),
            Map.entry("*", TokenType.STAR),
            Map.entry("/", TokenType.SLASH),
            Map.entry("%", TokenType.PERCENT),
            Map.entry("=", TokenType.ASSIGN),
            Map.entry("==", TokenType.EQ),
            Map.entry("!=", TokenType.NEQ),
            Map.entry("<", TokenType.LT),
            Map.entry(">", TokenType.GT),
            Map.entry("<=", TokenType.LE),
            Map.entry(">=", TokenType.GE),
            Map.entry("&&", TokenType.AND),
            Map.entry("||", TokenType.OR),
            Map.entry("!", TokenType.NOT),
            Map.entry("++", TokenType.INC),
            Map.entry("--", TokenType.DEC),
            Map.entry("+=", TokenType.PLUS_ASSIGN),
            Map.entry("-=", TokenType.MINUS_ASSIGN),
            Map.entry("*=", TokenType.STAR_ASSIGN),
            Map.entry("/=", TokenType.SLASH_ASSIGN),
            Map.entry("%=", TokenType.PERCENT_ASSIGN)
    );

    /**
     * 可能的操作符起始字符集合，用于快速判断当前字符是否可能是一个操作符的开始，
     * 从而决定是否进入操作符的读取逻辑
     */
    private static final Set<Character> OPERATOR_STARTS = Set.of(
            '+', '-', '*', '/', '%', '=', '!', '<', '>', '&', '|'
    );

    /**
     * 输入的源代码字符串，Lexer 将从这个字符串中逐字符扫描并生成 {@link Token} 序列
     */
    private final String source;

    /**
     * 符号表，记录标识符和常量的信息，包括词素、类型、首次出现的位置以及出现次数等，
     * 供语法分析器和后续编译阶段使用
     */
    private final SymbolTable symbolTable = new SymbolTable();

    /**
     * 当前扫描的位置索引，以及当前行号和列号，用于生成 {@link Token} 时记录位置信息，
     * 并在遇到错误时提供准确的错误位置
     */
    private int index = 0;

    /**
     * 当前扫描的行号，初始值为 1，表示源代码的第一行
     */
    private int line = 1;

    /**
     * 当前扫描的列号，初始值为 1，表示每行的第一列
     */
    private int column = 1;

    /**
     * 构造函数，接受源代码字符串作为输入
     *
     * @param source 输入的源代码字符串，如果为 {@code null} 则使用空字符串
     */
    public Lexer(String source) {
        this.source = source == null ? "" : source;
    }

    /**
     * 扫描完整源代码，返回可供语法分析器使用的 Token 序列。
     *
     * <p>词法层会识别部分当前文法尚未消费的 C++ 风格词素，例如 {@code for}、自增自减和复合赋值；
     * 这些 Token 会在语法阶段被拒绝。
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            char current = peek();

            if (Character.isWhitespace(current)) {
                skipWhitespace();
                continue;
            }

            int startLine = line;
            int startColumn = column;

            if (isIdentifierStart(current)) { // 以字母或下划线开头，可能是标识符或关键字
                tokens.add(readIdentifierOrKeyword(startLine, startColumn));
            } else if (Character.isDigit(current)) { // 以数字开头，可能是数字字面量
                tokens.add(readNumber(startLine, startColumn));
            } else if (current == '"') { // 以双引号开头，可能是字符串字面量
                tokens.add(readStringLiteral(startLine, startColumn));
            } else if (current == '\'') { // 以单引号开头，可能是字符字面量
                tokens.add(readCharLiteral(startLine, startColumn));
            } else if (current == '/' && matchNext('/')) { // 以 // 开头，可能是单行注释
                skipLineComment();
            } else if (current == '/' && matchNext('*')) { // 以 /* 开头，可能是多行注释
                skipBlockComment();
            } else if (OPERATOR_STARTS.contains(current)) { // 可能是操作符，进入操作符的读取逻辑
                tokens.add(readOperator(startLine, startColumn));
            } else { // 其他情况，尝试读取分隔符或未知字符
                tokens.add(readSeparatorOrUnknown(startLine, startColumn));
            }
        }

        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    /**
     * 获取词法阶段记录的标识符和常量表。
     *
     * @return 本次词法分析的符号表
     */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * 读取标识符或关键字
     *
     * @param startLine   标识符或关键字的起始行号
     * @param startColumn 标识符或关键字的起始列号
     * @return 生成的 {@link Token} 对象，类型为 {@code IDENTIFIER} 或对应的关键字类型
     */
    private Token readIdentifierOrKeyword(int startLine, int startColumn) {
        StringBuilder builder = new StringBuilder();
        while (!isAtEnd() && isIdentifierPart(peek())) { // 继续读取直到遇到非标识符字符
            builder.append(advance());
        }

        String lexeme = builder.toString(); // 获取完整的标识符或关键字文本
        // 判断是否是关键字，如果不是则默认为标识符
        TokenType type = KEYWORDS.getOrDefault(lexeme, TokenType.IDENTIFIER);
        if (type == TokenType.IDENTIFIER) {
            symbolTable.recordIdentifier(lexeme, startLine, startColumn);
        }
        return new Token(type, lexeme, startLine, startColumn);
    }

    /**
     * 读取数字字面量，支持整数、浮点数和科学计数法表示的数字
     *
     * @param startLine   数字字面量的起始行号
     * @param startColumn 数字字面量的起始列号
     * @return 生成的 {@link Token} 对象，类型为 {@code INT_LITERAL} 或 {@code FLOAT_LITERAL}，具体取决于数字的格式
     */
    private Token readNumber(int startLine, int startColumn) {
        StringBuilder builder = new StringBuilder();
        while (!isAtEnd() && Character.isDigit(peek())) {
            builder.append(advance());
        }

        // 判断是否是浮点数
        boolean isFloat = false;
        if (!isAtEnd() && peek() == '.' && Character.isDigit(peekNext())) {
            isFloat = true;
            do {
                builder.append(advance());
            } while (!isAtEnd() && Character.isDigit(peek()));
        }

        // 判断是否是科学计数法
        if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
            isFloat = true;
            builder.append(advance());
            if (!isAtEnd() && (peek() == '+' || peek() == '-')) {
                builder.append(advance());
            }
            if (isAtEnd() || !Character.isDigit(peek())) {
                throw error("科学计数法缺少指数数字", startLine, startColumn);
            }
            while (!isAtEnd() && Character.isDigit(peek())) {
                builder.append(advance());
            }
        }

        TokenType type = isFloat ? TokenType.FLOAT_LITERAL : TokenType.INT_LITERAL;
        String lexeme = builder.toString();
        symbolTable.recordConstant(lexeme, type, startLine, startColumn);
        return new Token(type, lexeme, startLine, startColumn);
    }

    /**
     * 读取字符串字面量，支持转义字符，并确保字符串常量不能直接跨行
     *
     * @param startLine   字符串字面量的起始行号
     * @param startColumn 字符串字面量的起始列号
     * @return 生成的 {@link Token} 对象，类型为 {@code STRING_LITERAL}，包含完整的字符串文本
     */
    private Token readStringLiteral(int startLine, int startColumn) {
        StringBuilder builder = new StringBuilder();
        builder.append(advance());

        while (!isAtEnd()) {
            char current = advance();
            builder.append(current);
            if (current == '\\') { // 处理转义字符
                if (isAtEnd()) {
                    break;
                }
                builder.append(advance());
            } else if (current == '"') { // 字符串常量结束
                String lexeme = builder.toString();
                symbolTable.recordConstant(lexeme, TokenType.STRING_LITERAL, startLine, startColumn);
                return new Token(TokenType.STRING_LITERAL, lexeme, startLine, startColumn);
            } else if (current == '\n') {
                throw error("字符串常量不能直接跨行", startLine, startColumn);
            }
        }

        throw error("字符串常量缺少结束双引号", startLine, startColumn);
    }

    /**
     * 读取字符字面量，支持转义字符，并确保字符常量只能包含一个字符（或一个转义序列）
     *
     * @param startLine   字符字面量的起始行号
     * @param startColumn 字符字面量的起始列号
     * @return 生成的 {@link Token} 对象，类型为 {@code CHAR_LITERAL}，包含完整的字符文本
     */
    private Token readCharLiteral(int startLine, int startColumn) {
        StringBuilder builder = new StringBuilder();
        builder.append(advance());

        if (isAtEnd() || peek() == '\n') {
            throw error("字符常量缺少内容", startLine, startColumn);
        }

        char current = advance();
        builder.append(current);
        if (current == '\\') { // 处理转义字符
            if (isAtEnd() || peek() == '\n') {
                throw error("字符转义序列不完整", startLine, startColumn);
            }
            builder.append(advance());
        }

        if (isAtEnd() || peek() != '\'') {
            throw error("字符常量缺少结束单引号", startLine, startColumn);
        }
        builder.append(advance());

        String lexeme = builder.toString();
        symbolTable.recordConstant(lexeme, TokenType.CHAR_LITERAL, startLine, startColumn);
        return new Token(TokenType.CHAR_LITERAL, lexeme, startLine, startColumn);
    }

    /**
     * 读取操作符，支持单字符和双字符的操作符，并根据预定义的 OPERATORS 映射表生成对应的 Token
     *
     * @param startLine   操作符的起始行号
     * @param startColumn 操作符的起始列号
     * @return 生成的 {@link Token} 对象，类型根据 {@code OPERATORS} 映射表确定，
     * 如果不匹配任何已知操作符则返回类型为 {@code UNKNOWN} 的 {@link Token}
     */
    private Token readOperator(int startLine, int startColumn) {
        char first = advance();
        String twoChars = "" + first + peekOrNull();
        if (OPERATORS.containsKey(twoChars)) {
            advance();
            return new Token(OPERATORS.get(twoChars), twoChars, startLine, startColumn);
        }

        String oneChar = String.valueOf(first);
        TokenType type = OPERATORS.get(oneChar);
        return new Token(Objects.requireNonNullElse(type, TokenType.UNKNOWN), oneChar, startLine, startColumn);
    }

    /**
     * 读取分隔符或未知字符，支持常见的分隔符如括号、逗号、分号等，并根据预定义的字符生成对应的 Token
     *
     * @param startLine   分隔符或未知字符的起始行号
     * @param startColumn 分隔符或未知字符的起始列号
     * @return 生成的 {@link Token} 对象，类型根据预定义的字符确定，
     * 如果不匹配任何已知分隔符则返回类型为 {@code UNKNOWN} 的 {@link Token}
     */
    private Token readSeparatorOrUnknown(int startLine, int startColumn) {
        char current = advance();
        return switch (current) {
            case '(' -> new Token(TokenType.LPAREN, "(", startLine, startColumn);
            case ')' -> new Token(TokenType.RPAREN, ")", startLine, startColumn);
            case '{' -> new Token(TokenType.LBRACE, "{", startLine, startColumn);
            case '}' -> new Token(TokenType.RBRACE, "}", startLine, startColumn);
            case '[' -> new Token(TokenType.LBRACKET, "[", startLine, startColumn);
            case ']' -> new Token(TokenType.RBRACKET, "]", startLine, startColumn);
            case ',' -> new Token(TokenType.COMMA, ",", startLine, startColumn);
            case ';' -> new Token(TokenType.SEMICOLON, ";", startLine, startColumn);
            case ':' -> new Token(TokenType.COLON, ":", startLine, startColumn);
            case '.' -> new Token(TokenType.DOT, ".", startLine, startColumn);
            default -> new Token(TokenType.UNKNOWN, String.valueOf(current), startLine, startColumn);
        };
    }

    /**
     * 跳过连续的空白字符，包括空格、制表符、换行符等，直到遇到非空白字符或扫描到输入字符串的末尾
     */
    private void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(peek())) {
            advance();
        }
    }

    /**
     * 跳过单行注释，从当前扫描位置开始一直扫描到遇到换行符或扫描到输入字符串的末尾，期间忽略所有字符
     */
    private void skipLineComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    /**
     * 跳过多行注释，从当前扫描位置开始一直扫描到遇到结束符或扫描到输入字符串的末尾，
     */
    private void skipBlockComment() {
        int startLine = line;
        int startColumn = column;
        advance();
        advance();

        while (!isAtEnd()) {
            if (peek() == '*' && matchNext('/')) {
                advance();
                advance();
                return;
            }
            advance();
        }

        throw error("多行注释缺少结束符 */", startLine, startColumn);
    }

    /**
     * 判断是否已经扫描到输入字符串的末尾
     *
     * @return 是否已经扫描到输入字符串的末尾
     */
    private boolean isAtEnd() {
        return index >= source.length();
    }

    /**
     * 返回当前扫描位置的字符，但不移动扫描位置
     *
     * @return 当前扫描位置的字符，如果已经扫描到末尾则返回 {@code '\0'} 作为结束标志
     */
    private char peek() {
        return source.charAt(index);
    }

    /**
     * 返回下一个扫描位置的字符，但不移动扫描位置
     *
     * @return 下一个扫描位置的字符，如果已经扫描到末尾则返回 {@code '\0'} 作为结束标志
     */
    private char peekNext() {
        if (index + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(index + 1);
    }

    /**
     * 返回当前扫描位置的字符
     *
     * @return 当前扫描位置的字符，如果已经扫描到末尾则返回 {@code '\0'} 作为结束标志
     */
    private char peekOrNull() {
        return isAtEnd() ? '\0' : peek();
    }

    /**
     * 判断下一个扫描位置的字符是否与预期字符匹配
     *
     * @param expected 预期的字符
     * @return 下一个扫描位置的字符是否与预期字符匹配
     */
    private boolean matchNext(char expected) {
        return index + 1 < source.length() && source.charAt(index + 1) == expected;
    }

    /**
     * 返回当前扫描位置的字符，并将扫描位置移动到下一个字符，同时更新行号和列号信息
     *
     * @return 当前扫描位置的字符，如果已经扫描到末尾则返回 {@code '\0'} 作为结束标志
     */
    private char advance() {
        char current = source.charAt(index++);
        if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return current;
    }

    /**
     * 判断字符是否可以作为标识符的起始字符，通常是字母或下划线
     *
     * @param c 待判断的字符
     * @return 字符是否可以作为标识符的起始字符
     */
    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    /**
     * 判断字符是否可以作为标识符的组成部分，通常是字母、数字或下划线
     *
     * @param c 待判断的字符
     * @return 字符是否可以作为标识符的组成部分
     */
    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * 生成一个包含错误信息和错误位置的 LexerException，用于在词法分析过程中遇到错误时抛出异常
     *
     * @param message     错误信息描述，说明具体的错误类型和原因
     * @param errorLine   错误发生的行号
     * @param errorColumn 错误发生的列号
     * @return 包含错误信息和错误位置的 LexerException 对象
     * @see LexerException
     */
    private LexerException error(String message, int errorLine, int errorColumn) {
        return new LexerException(message + "，位置：第 " + errorLine + " 行，第 " + errorColumn + " 列");
    }
}
