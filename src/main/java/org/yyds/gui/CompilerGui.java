package org.yyds.gui;

import org.yyds.codegen.TargetInstruction;
import org.yyds.ir.Quadruple;
import org.yyds.lexer.Symbol;
import org.yyds.lexer.Token;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 编译器的 Swing 可视化入口，提供源码输入区和各编译阶段输出标签页。
 */
public class CompilerGui extends JFrame {
    private static final double UI_SCALE = 1.25;
    private static final String CODE_FONT_NAME = "Maple Mono NF CN";
    private static final String UI_FONT_NAME = "Microsoft YaHei UI";
    private static final Font SOURCE_FONT = font(CODE_FONT_NAME, Font.PLAIN, 14);
    private static final Font OUTPUT_FONT = font(CODE_FONT_NAME, Font.PLAIN, 13);
    private static final Font UI_FONT = font(UI_FONT_NAME, Font.PLAIN, 13);
    private static final int WINDOW_WIDTH = scale(800);
    private static final int WINDOW_HEIGHT = scale(600);
    private static final int SOURCE_COLUMNS = 48;
    private static final int SOURCE_ROWS = 30;
    private static final int TABLE_ROW_HEIGHT = scale(24);
    private static final int GAP = scale(8);
    private static final int SOURCE_WIDTH = scale(460);
    private static final int SOURCE_HEIGHT = scale(700);

    private static final String SAMPLE_SOURCE = """
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

    private final CompilerPipeline pipeline = new CompilerPipeline();
    private final JTextArea sourceArea = new JTextArea(SAMPLE_SOURCE, SOURCE_ROWS, SOURCE_COLUMNS);
    private final JTextArea lineNumberArea = new JTextArea();
    private final Map<String, JTable> outputTables = new LinkedHashMap<>();
    private final Map<String, JTextArea> outputTexts = new LinkedHashMap<>();
    private final JTabbedPane outputTabs = new JTabbedPane();
    private final JLabel statusLabel = new JLabel("就绪");

    public CompilerGui() {
        super("C++ 子集编译器 - 可视化演示");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(GAP, GAP));

        add(createToolbar(), BorderLayout.NORTH);
        add(createContentPane(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new CompilerGui().setVisible(true);
        });
    }

    private JPanel createToolbar() {
        JButton compileButton = new JButton("运行编译");
        JButton sampleButton = new JButton("加载示例");
        JButton copyButton = new JButton("复制当前输出");
        JButton clearButton = new JButton("清空输出");
        applyUIFont(compileButton);
        applyUIFont(sampleButton);
        applyUIFont(copyButton);
        applyUIFont(clearButton);

        compileButton.addActionListener(event -> compileSource());
        sampleButton.addActionListener(event -> {
            sourceArea.setText(SAMPLE_SOURCE);
            statusLabel.setText("已加载示例源码");
        });
        copyButton.addActionListener(event -> copyCurrentOutput());
        clearButton.addActionListener(event -> clearOutputs());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, 0));
        buttons.add(compileButton);
        buttons.add(sampleButton);
        buttons.add(copyButton);
        buttons.add(clearButton);

        JPanel toolbar = new JPanel(new BorderLayout(scale(12), 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, 0, GAP));
        JLabel titleLabel = new JLabel("C++ 子集编译器");
        applyUIFont(titleLabel);
        toolbar.add(titleLabel, BorderLayout.WEST);
        toolbar.add(buttons, BorderLayout.EAST);
        return toolbar;
    }

    private JSplitPane createContentPane() {
        sourceArea.setFont(SOURCE_FONT);
        sourceArea.setTabSize(4);
        configureLineNumberArea();

        JScrollPane sourceScrollPane = new JScrollPane(sourceArea);
        sourceScrollPane.setRowHeaderView(lineNumberArea);
        sourceScrollPane.setBorder(BorderFactory.createTitledBorder("源代码输入"));
        sourceScrollPane.setPreferredSize(new Dimension(SOURCE_WIDTH, SOURCE_HEIGHT));

        addTableTab("Token 序列");
        addTableTab("标识符表");
        addTableTab("常量表");
        addTextTab("语法分析树");
        addTableTab("中间代码四元式");
        addTableTab("优化后四元式");
        addTableTab("目标代码");
        addTextTab("错误信息");

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourceScrollPane, outputTabs);
        splitPane.setResizeWeight(0.38);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, GAP, 0, GAP));
        return splitPane;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(scale(4), scale(10), scale(6), scale(10)));
        statusLabel.setFont(UI_FONT);
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    private void configureLineNumberArea() {
        lineNumberArea.setEditable(false);
        lineNumberArea.setFocusable(false);
        lineNumberArea.setFont(SOURCE_FONT);
        lineNumberArea.setBackground(UIManager.getColor("Panel.background"));
        lineNumberArea.setForeground(Color.GRAY);
        lineNumberArea.setBorder(BorderFactory.createEmptyBorder(0, scale(6), 0, scale(6)));
        sourceArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateLineNumbers();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateLineNumbers();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateLineNumbers();
            }
        });
        updateLineNumbers();
    }

    private void updateLineNumbers() {
        int lineCount = Math.max(1, sourceArea.getLineCount());
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            builder.append(String.format("%" + String.valueOf(lineCount).length() + "d", i)).append(System.lineSeparator());
        }
        lineNumberArea.setText(builder.toString());
    }

    private void addTableTab(String title) {
        JTable table = new JTable(nonEditableModel(new String[]{"内容"}));
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setFont(OUTPUT_FONT);
        table.getTableHeader().setFont(UI_FONT);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        outputTables.put(title, table);
        outputTabs.addTab(title, new JScrollPane(table));
    }

    private void addTextTab(String title) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(OUTPUT_FONT);
        outputTexts.put(title, area);
        outputTabs.addTab(title, new JScrollPane(area));
    }

    private void compileSource() {
        clearOutputContents();
        statusLabel.setText("正在编译...");
        try {
            CompilationResult result = pipeline.compile(sourceArea.getText());
            setTokenTable(result.tokens());
            setSymbolTable("标识符表", result.identifiers());
            setSymbolTable("常量表", result.constants());
            outputTexts.get("语法分析树").setText(result.parseTree());
            setQuadrupleTable("中间代码四元式", result.quadruples());
            setQuadrupleTable("优化后四元式", result.optimizedQuadruples());
            setTargetCodeTable(result.targetCode());
            outputTexts.get("错误信息").setText("编译成功，无错误。");
            statusLabel.setText("编译成功：Token " + result.tokens().size()
                    + " 个，原始四元式 " + result.quadruples().size()
                    + " 条，优化后 " + result.optimizedQuadruples().size()
                    + " 条，目标指令 " + result.targetCode().size()
                    + " 条，用时 " + result.elapsedMillis() + " ms");
            outputTabs.setSelectedIndex(0);
        } catch (Exception exception) {
            clearOutputContents();
            outputTexts.get("错误信息").setText(formatException(exception));
            statusLabel.setText("编译失败：" + exception.getClass().getSimpleName());
            outputTabs.setSelectedIndex(outputTabs.indexOfTab("错误信息"));
        }
    }

    private void setTokenTable(List<Token> tokens) {
        DefaultTableModel model = nonEditableModel(new String[]{"序号", "类型", "词素", "行", "列"});
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            model.addRow(new Object[]{i + 1, token.type(), token.lexeme(), token.line(), token.column()});
        }
        outputTables.get("Token 序列").setModel(model);
    }

    private void setSymbolTable(String tabName, List<Symbol> symbols) {
        DefaultTableModel model = nonEditableModel(new String[]{"序号", "词素", "类型", "首次行", "首次列", "出现次数"});
        for (int i = 0; i < symbols.size(); i++) {
            Symbol symbol = symbols.get(i);
            model.addRow(new Object[]{i + 1, symbol.getLexeme(), symbol.getType(), symbol.getLine(), symbol.getColumn(), symbol.getOccurrences()});
        }
        outputTables.get(tabName).setModel(model);
    }

    private void setQuadrupleTable(String tabName, List<Quadruple> quadruples) {
        DefaultTableModel model = nonEditableModel(new String[]{"序号", "op", "arg1", "arg2", "result"});
        for (int i = 0; i < quadruples.size(); i++) {
            Quadruple quadruple = quadruples.get(i);
            model.addRow(new Object[]{i + 1, display(quadruple.op()), display(quadruple.arg1()), display(quadruple.arg2()), display(quadruple.result())});
        }
        outputTables.get(tabName).setModel(model);
    }

    private void setTargetCodeTable(List<TargetInstruction> instructions) {
        DefaultTableModel model = nonEditableModel(new String[]{"序号", "指令", "操作数", "文本"});
        for (int i = 0; i < instructions.size(); i++) {
            TargetInstruction instruction = instructions.get(i);
            model.addRow(new Object[]{i + 1, instruction.op(), String.join(", ", instruction.operands()), instruction.toString()});
        }
        outputTables.get("目标代码").setModel(model);
    }

    private DefaultTableModel nonEditableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void clearOutputs() {
        clearOutputContents();
        statusLabel.setText("输出已清空");
    }

    private void clearOutputContents() {
        for (JTable table : outputTables.values()) {
            table.setModel(nonEditableModel(new String[]{"内容"}));
        }
        for (JTextArea area : outputTexts.values()) {
            area.setText("");
        }
    }

    private void copyCurrentOutput() {
        String title = outputTabs.getTitleAt(outputTabs.getSelectedIndex());
        String text;
        JTable table = outputTables.get(title);
        if (table != null) {
            text = tableToText(table);
        } else {
            JTextArea area = outputTexts.get(title);
            text = area == null ? "" : area.getText();
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        statusLabel.setText("已复制当前输出：" + title);
    }

    private String tableToText(JTable table) {
        StringBuilder builder = new StringBuilder();
        for (int column = 0; column < table.getColumnCount(); column++) {
            if (column > 0) {
                builder.append('\t');
            }
            builder.append(table.getColumnName(column));
        }
        builder.append(System.lineSeparator());
        for (int row = 0; row < table.getRowCount(); row++) {
            for (int column = 0; column < table.getColumnCount(); column++) {
                if (column > 0) {
                    builder.append('\t');
                }
                Object value = table.getValueAt(row, column);
                builder.append(value == null ? "" : value);
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String formatException(Exception exception) {
        StringBuilder builder = new StringBuilder();
        builder.append("编译失败").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("类型：").append(exception.getClass().getSimpleName()).append(System.lineSeparator());
        builder.append("信息：").append(exception.getMessage()).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("调用栈：").append(System.lineSeparator());
        for (StackTraceElement element : exception.getStackTrace()) {
            if (element.getClassName().startsWith("org.yyds")) {
                builder.append("    at ").append(element).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static int scale(int value) {
        return (int) Math.round(value * UI_SCALE);
    }

    private static Font font(String name, int style, int size) {
        return new Font(name, style, scale(size));
    }

    private void applyUIFont(java.awt.Component component) {
        component.setFont(UI_FONT);
    }

    private String join(List<String> values) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String value : values) {
            joiner.add(value);
        }
        return joiner.toString();
    }
}
