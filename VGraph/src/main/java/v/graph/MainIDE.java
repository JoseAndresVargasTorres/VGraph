package v.graph;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Importaciones de ANTLR
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

public class MainIDE extends JFrame {
    private JTextArea codeArea;
    private JTextArea lineNumbers;
    private JTextArea outputArea;
    private JButton compileButton;
    private JButton loadButton;
    private JButton saveButton;
    private JButton runButton;
    private JFileChooser fileChooser;
    private File currentFile;
    private static final String EXTENSION = "vgraph";

    // ErrorListener simple para capturar errores
    private static class SimpleErrorListener implements ANTLRErrorListener {
        private List<String> errors = new ArrayList<>();
        private JTextArea output;

        public SimpleErrorListener(JTextArea output) {
            this.output = output;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {

            // Mejorar y traducir mensajes de error
            String errorMsg = "Error at line " + line + ":" + charPositionInLine + " - ";

            // Traducir mensajes comunes de ANTLR
            if (msg.contains("missing ';'")) {
                errorMsg += "Missing semicolon";
            } else if (msg.contains("missing ')'")) {
                errorMsg += "Missing closing parenthesis ')'";
            } else if (msg.contains("missing '('")) {
                errorMsg += "Missing opening parenthesis '('";
            } else if (msg.contains("missing '}'")) {
                errorMsg += "Missing closing brace '}'";
            } else if (msg.contains("missing '{'")) {
                errorMsg += "Missing opening brace '{'";
            } else if (msg.contains("mismatched input")) {
                errorMsg += "Unexpected token '" + getTokenText(offendingSymbol) + "'";
            } else if (msg.contains("expecting")) {
                errorMsg += "Expected valid expression or statement";
            } else {
                // Mensaje genérico en inglés
                errorMsg += msg.replace("extraneous input", "unexpected input")
                        .replace("no viable alternative", "invalid syntax");
            }

            errors.add(errorMsg);
            if (output != null) {
                output.append(errorMsg + "\n");
            }
        }

        private String getTokenText(Object token) {
            if (token instanceof Token) {
                return ((Token) token).getText();
            }
            return "unknown";
        }

        @Override
        public void reportAmbiguity(Parser parser, DFA dfa, int startIndex, int stopIndex,
                                    boolean exact, java.util.BitSet ambigAlts, ATNConfigSet configs) {}

        @Override
        public void reportAttemptingFullContext(Parser parser, DFA dfa, int startIndex, int stopIndex,
                                                java.util.BitSet conflictingAlts, ATNConfigSet configs) {}

        @Override
        public void reportContextSensitivity(Parser parser, DFA dfa, int startIndex, int stopIndex,
                                             int prediction, ATNConfigSet configs) {}

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public void clear() {
            errors.clear();
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public MainIDE() {
        setTitle("VGraph IDE");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Crear área de código con números de línea
        codeArea = new JTextArea();
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumbers = new JTextArea("1");
        lineNumbers.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumbers.setBackground(new Color(240, 240, 240));
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // Configurar Ctrl+Z para deshacer
        setupUndoRedo();

        // Actualizar números de línea cuando cambie el texto
        codeArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLineNumbers();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLineNumbers();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLineNumbers();
            }
        });

        // Crear área de salida/errores
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane outputPane = new JScrollPane(outputArea);
        outputPane.setPreferredSize(new Dimension(getWidth(), 200));

        // Panel de código con números de línea
        JScrollPane codeScrollPane = new JScrollPane(codeArea);
        codeScrollPane.setRowHeaderView(lineNumbers);

        // Crear toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        loadButton = new JButton("Load");
        saveButton = new JButton("Save");
        compileButton = new JButton("Compile");
        runButton = new JButton("Run");

        toolbar.add(loadButton);
        toolbar.add(saveButton);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(compileButton);
        toolbar.add(runButton);

        // Configurar file chooser
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("VGraph Files (." + EXTENSION + ")", EXTENSION));

        // Agregar componentes al frame
        add(toolbar, BorderLayout.NORTH);
        add(codeScrollPane, BorderLayout.CENTER);
        add(outputPane, BorderLayout.SOUTH);

        // Configurar eventos de botones
        loadButton.addActionListener(e -> loadFile());
        saveButton.addActionListener(e -> saveFile());
        compileButton.addActionListener(e -> compileCode());
        runButton.addActionListener(e -> executeCode());
    }

    // Configurar Ctrl+Z para deshacer y Ctrl+Y para rehacer
    private void setupUndoRedo() {
        UndoManager undoManager = new UndoManager();
        Document document = codeArea.getDocument();

        // Agregar listener para cambios en el documento
        document.addUndoableEditListener(undoManager);

        // Configurar acciones de teclado
        InputMap inputMap = codeArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = codeArea.getActionMap();

        // Ctrl+Z para deshacer
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });

        // Ctrl+Y para rehacer
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        });
    }

    private void updateLineNumbers() {
        try {
            int lineCount = codeArea.getDocument().getDefaultRootElement().getElementCount();
            StringBuilder numbers = new StringBuilder();
            for (int i = 1; i <= lineCount; i++) {
                numbers.append(i).append("\n");
            }
            if (!lineNumbers.getText().equals(numbers.toString())) {
                lineNumbers.setText(numbers.toString());
            }
        } catch (Exception e) {
            // Ignorar errores
        }
    }

    private void loadFile() {
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                currentFile = fileChooser.getSelectedFile();
                BufferedReader reader = new BufferedReader(new FileReader(currentFile));
                StringBuilder content = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }

                reader.close();
                codeArea.setText(content.toString());
                updateLineNumbers();
                outputArea.setText("");
                setTitle("VGraph IDE - " + currentFile.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                if (!currentFile.getName().endsWith("." + EXTENSION)) {
                    currentFile = new File(currentFile.getAbsolutePath() + "." + EXTENSION);
                }
                setTitle("VGraph IDE - " + currentFile.getName());
            } else {
                return;
            }
        }

        try {
            FileWriter writer = new FileWriter(currentFile);
            writer.write(codeArea.getText());
            writer.close();
            JOptionPane.showMessageDialog(this, "File saved successfully.",
                    "Save", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void compileCode() {
        outputArea.setText("");
        outputArea.setForeground(Color.BLACK);

        if (codeArea.getText().trim().isEmpty()) {
            outputArea.setForeground(Color.RED);
            outputArea.setText("No code to compile.");
            return;
        }

        try {
            // Limpiar resaltado anterior
            clearHighlights();

            // Lista para recolectar TODOS los errores
            List<String> allErrors = new ArrayList<>();

            // Crear input stream del código
            CharStream input = CharStreams.fromString(codeArea.getText());

            // ANÁLISIS LÉXICO
            VGraphLexer lexer = new VGraphLexer(input);
            SimpleErrorListener lexerErrorListener = new SimpleErrorListener(null);
            lexer.removeErrorListeners();
            lexer.addErrorListener(lexerErrorListener);

            // ANÁLISIS SINTÁCTICO
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            VGraphParser parser = new VGraphParser(tokens);
            SimpleErrorListener parserErrorListener = new SimpleErrorListener(null);
            parser.removeErrorListeners();
            parser.addErrorListener(parserErrorListener);

            // Intentar parsear SIEMPRE para encontrar todos los errores sintácticos
            VGraphParser.ProgramContext tree = null;
            try {
                tree = parser.program();
            } catch (Exception e) {
                // Continúa aunque falle el parsing para mostrar errores
            }

            // Recolectar errores léxicos y sintácticos
            allErrors.addAll(lexerErrorListener.getErrors());
            allErrors.addAll(parserErrorListener.getErrors());

            // ANÁLISIS SEMÁNTICO - Hacer SIEMPRE, incluso si hay errores sintácticos
            if (tree != null) {
                try {
                    SemanticValidator semanticValidator = new SemanticValidator();
                    semanticValidator.visit(tree);
                    allErrors.addAll(semanticValidator.getSemanticErrors());
                } catch (Exception e) {
                    // Si falla el análisis semántico, agregar error genérico
                    allErrors.add("Error at line 1 - Semantic analysis failed due to syntax errors");
                }
            }

            // Mostrar TODOS los errores encontrados
            if (!allErrors.isEmpty()) {
                outputArea.setForeground(Color.RED);
                outputArea.setText("ERRORS FOUND (" + allErrors.size() + "):\n\n");

                // Mostrar todos los errores numerados
                for (int i = 0; i < allErrors.size(); i++) {
                    outputArea.append((i + 1) + ". " + allErrors.get(i) + "\n");
                }

                // Resaltar TODAS las líneas con errores
                highlightAllErrorLines(allErrors);
            } else {
                outputArea.setForeground(new Color(0, 150, 0));
                outputArea.setText("BUILD SUCCESSFUL\n\n");
                outputArea.append("Code compiled successfully with no errors.\n");
                outputArea.append("Press 'Run' to generate C code.");
            }

        } catch (Exception e) {
            outputArea.setForeground(Color.RED);
            outputArea.setText("COMPILATION ERROR:\n\n" + e.getMessage());
        }
    }

    private void executeCode() {
        outputArea.setText("");
        outputArea.setForeground(Color.BLACK);

        if (codeArea.getText().trim().isEmpty()) {
            outputArea.setForeground(Color.RED);
            outputArea.setText("No code to execute.");
            return;
        }

        try {
            // Limpiar resaltado anterior
            clearHighlights();

            // Verificar errores antes de ejecutar
            CharStream input = CharStreams.fromString(codeArea.getText());
            VGraphLexer lexer = new VGraphLexer(input);
            SimpleErrorListener errorListener = new SimpleErrorListener(null);
            lexer.removeErrorListeners();
            lexer.addErrorListener(errorListener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            VGraphParser parser = new VGraphParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            VGraphParser.ProgramContext tree = parser.program();

            // Recolectar todos los errores
            List<String> allErrors = new ArrayList<>();
            allErrors.addAll(errorListener.getErrors());

            // Si no hay errores sintácticos, verificar semánticos
            if (allErrors.isEmpty()) {
                SemanticValidator semanticValidator = new SemanticValidator();
                semanticValidator.visit(tree);
                allErrors.addAll(semanticValidator.getSemanticErrors());
            }

            // Si hay errores, no ejecutar
            if (!allErrors.isEmpty()) {
                outputArea.setForeground(Color.RED);
                outputArea.setText("Cannot execute code with errors.\n\n");
                outputArea.append("Errors found:\n");

                for (String error : allErrors) {
                    outputArea.append("• " + error + "\n");
                }

                outputArea.append("\nCompile the code to see all errors.");
                highlightAllErrorLines(allErrors);
                return;
            }

            // Si no hay errores, generar código C
            VGraphCustomVisitor visitor = new VGraphCustomVisitor();
            String cCode = visitor.visit(tree);

            // Mostrar código C generado
            outputArea.setForeground(Color.BLUE);
            outputArea.setText("C CODE GENERATED:\n");
            outputArea.append("=" + "=".repeat(50) + "\n\n");
            outputArea.append(cCode);
            outputArea.append("\n" + "=".repeat(52) + "\n");
            outputArea.append("Code ready to transfer to FPGA.");

        } catch (Exception e) {
            outputArea.setForeground(Color.RED);
            outputArea.setText("EXECUTION ERROR:\n\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método unificado para resaltar errores - CORREGIDO
    private void highlightAllErrorLines(List<String> errors) {
        Highlighter highlighter = codeArea.getHighlighter();

        try {
            // Color rojo claro para resaltar errores
            Highlighter.HighlightPainter painter =
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200));

            for (String error : errors) {
                // Buscar patrón "line X" o "line X:Y"
                Pattern pattern = Pattern.compile("line (\\d+)");
                Matcher matcher = pattern.matcher(error);

                if (matcher.find()) {
                    try {
                        int lineNum = Integer.parseInt(matcher.group(1));
                        if (lineNum > 0 && lineNum <= codeArea.getLineCount()) {
                            int startOffset = codeArea.getLineStartOffset(lineNum - 1);
                            int endOffset = codeArea.getLineEndOffset(lineNum - 1);
                            highlighter.addHighlight(startOffset, endOffset, painter);
                        }
                    } catch (Exception e) {
                        // Ignorar errores de parsing del número de línea
                    }
                }
            }
        } catch (Exception e) {
            // Ignorar errores de resaltado
        }
    }

    private void clearHighlights() {
        codeArea.getHighlighter().removeAllHighlights();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new MainIDE());
    }
}