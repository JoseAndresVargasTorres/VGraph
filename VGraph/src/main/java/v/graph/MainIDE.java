package v.graph;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
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
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.misc.Interval;

public class MainIDE extends JFrame {
    private JTextArea codeArea;
    private JTextArea lineNumbers;
    private JTextArea errorArea;
    private JButton compileButton;
    private JButton loadButton;
    private JButton saveButton;
    private JButton runButton;
    private JFileChooser fileChooser;
    private File currentFile;
    private static final String EXTENSION = "vgraph";
    private static final String DIRBASE = "src/test/resources/";

    // Clase personalizada para capturar y manejar errores de ANTLR
    private static class VGraphErrorListener implements ANTLRErrorListener {
        private List<String> syntaxErrors = new ArrayList<>();
        private JTextArea errorOutput;
        private String sourceCode;

        public VGraphErrorListener(JTextArea errorOutput, String sourceCode) {
            this.errorOutput = errorOutput;
            this.sourceCode = sourceCode;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            String errorMsg = "Error en línea " + line + ":" + charPositionInLine + " - " + msg;

            // Recuperar el contexto de la línea para mostrar el error
            String lineContext = getLineFromSource(line);
            if (lineContext != null) {
                errorMsg += "\nCódigo: " + lineContext.trim();
            }

            // Análisis más específico de errores comunes
            if (msg.contains("missing ';'") || msg.contains("mismatched input")) {
                errorMsg += "\nFalta un punto y coma (;) o hay un error de sintaxis en esta línea.";
            } else if (offendingSymbol != null && offendingSymbol instanceof Token) {
                Token token = (Token) offendingSymbol;

                // Verificar operaciones incompletas
                if (token.getType() == VGraphLexer.PLUS ||
                        token.getType() == VGraphLexer.MINUS ||
                        token.getType() == VGraphLexer.MULT ||
                        token.getType() == VGraphLexer.DIV ||
                        token.getType() == VGraphLexer.MODULUS) {
                    errorMsg += "\nOperación incompleta: falta el operando después de '" + token.getText() + "'";
                }

                // Verificar asignaciones incompletas
                if (token.getType() == VGraphLexer.ASSIGN &&
                        token.getText().equals("=") &&
                        msg.contains("expecting")) {
                    errorMsg += "\nAsignación incompleta: falta la expresión después del '='";
                }

                // Verificar estructura incorrecta de bucles
                if (token.getType() == VGraphLexer.LOOP) {
                    errorMsg += "\nPosible problema en la estructura del bucle. Revise la sintaxis:";
                    errorMsg += "\nloop (inicialización; comparación; incremento) { cuerpo }";
                }

                // Verificar estructura de condicionales
                if (token.getType() == VGraphLexer.IF ||
                        token.getType() == VGraphLexer.ELSE ||
                        token.getType() == VGraphLexer.ELSEIF) {
                    errorMsg += "\nPosible problema en la estructura del condicional.";
                }
            }

            syntaxErrors.add(errorMsg);

            // Imprimir error en área de errores
            if (errorOutput != null) {
                errorOutput.append(errorMsg + "\n\n");
            }
        }

        // Obtener el texto de la línea específica del código fuente
        private String getLineFromSource(int lineNumber) {
            if (sourceCode == null || sourceCode.isEmpty()) {
                return null;
            }

            String[] lines = sourceCode.split("\n");
            if (lineNumber > 0 && lineNumber <= lines.length) {
                return lines[lineNumber - 1];
            }
            return null;
        }

        @Override
        public void reportAmbiguity(Parser parser, DFA dfa, int startIndex, int stopIndex, boolean exact, java.util.BitSet ambigAlts, ATNConfigSet configs) {}

        @Override
        public void reportAttemptingFullContext(Parser parser, DFA dfa, int startIndex, int stopIndex, java.util.BitSet conflictingAlts, ATNConfigSet configs) {}

        @Override
        public void reportContextSensitivity(Parser parser, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {}

        public List<String> getSyntaxErrors() {
            return syntaxErrors;
        }

        public boolean hasErrors() {
            return !syntaxErrors.isEmpty();
        }

        public void clear() {
            syntaxErrors.clear();
            if (errorOutput != null) {
                errorOutput.setText("");
            }
        }
    }

    // Clase para realizar validaciones adicionales al código
    private class AdvancedCodeValidator {
        private String code;
        private VGraphErrorListener errorListener;

        public AdvancedCodeValidator(String code, VGraphErrorListener errorListener) {
            this.code = code;
            this.errorListener = errorListener;
        }

        public boolean validate() {
            errorListener.clear();

            try {
                // Validación inicial de estructura básica
                validateBasicStructure();

                // Análisis léxico y sintáctico estándar
                CharStream input = CharStreams.fromString(code);
                VGraphLexer lexer = new VGraphLexer(input);
                lexer.removeErrorListeners();
                lexer.addErrorListener(errorListener);

                CommonTokenStream tokens = new CommonTokenStream(lexer);
                VGraphParser parser = new VGraphParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(errorListener);

                // Verificaciones adicionales específicas de la gramática
                validateSpecificGrammarRules(tokens);

                // Ejecutar el parser
                try {
                    parser.program();
                } catch (Exception e) {
                    // Si el parser falla, ya se habrán registrado los errores a través del errorListener
                }

                // Si no se detectaron errores en el parsing, realizar análisis semántico adicional
                if (!errorListener.hasErrors()) {
                    validateSemanticRules();
                }

                return !errorListener.hasErrors();
            } catch (Exception e) {
                errorListener.syntaxError(null, null, -1, -1,
                        "Error interno durante la validación: " + e.getMessage(), null);
                e.printStackTrace();
                return false;
            }
        }

        // Validación básica de la estructura del código
        private void validateBasicStructure() {
            // Verificar balance de llaves
            int openBraces = countOccurrences(code, '{');
            int closeBraces = countOccurrences(code, '}');
            if (openBraces != closeBraces) {
                errorListener.syntaxError(null, null, 1, 0,
                        "Error en la estructura: Las llaves no están balanceadas. Hay " +
                                openBraces + " llaves abiertas y " + closeBraces + " llaves cerradas.", null);
            }

            // Verificar balance de paréntesis
            int openParens = countOccurrences(code, '(');
            int closeParens = countOccurrences(code, ')');
            if (openParens != closeParens) {
                errorListener.syntaxError(null, null, 1, 0,
                        "Error en la estructura: Los paréntesis no están balanceados. Hay " +
                                openParens + " paréntesis abiertos y " + closeParens + " paréntesis cerrados.", null);
            }

            // Verificar punto y coma al final de las sentencias
            validateSemicolons();
        }

        // Validación específica para reglas de la gramática
        private void validateSpecificGrammarRules(CommonTokenStream tokens) {
            // Analizar todos los tokens
            tokens.fill();
            List<Token> tokenList = tokens.getTokens();

            // Verificar asignaciones incompletas
            for (int i = 0; i < tokenList.size() - 1; i++) {
                Token token = tokenList.get(i);

                // Verificar operadores al final de línea
                if (isOperatorToken(token.getType())) {
                    // Verificar si es el último token en la línea
                    if (i + 1 < tokenList.size()) {
                        Token nextToken = tokenList.get(i + 1);
                        if (token.getLine() != nextToken.getLine() ||
                                nextToken.getType() == VGraphLexer.SEMICOLON) {
                            errorListener.syntaxError(null, token, token.getLine(), token.getCharPositionInLine(),
                                    "Operación incompleta: falta un operando después de '" + token.getText() + "'", null);
                        }
                    }
                }

                // Verificar bucles incompletos o malformados
                if (token.getType() == VGraphLexer.LOOP) {
                    validateLoopStructure(tokenList, i);
                }

                // Verificar condicionales incompletos
                if (token.getType() == VGraphLexer.IF) {
                    validateIfStructure(tokenList, i);
                }

                // Verificar declaraciones y asignaciones de variables
                if (token.getType() == VGraphLexer.ASSIGN) {
                    validateAssignment(tokenList, i);
                }
            }
        }

        // Validación específica para la estructura de bucles
        private void validateLoopStructure(List<Token> tokens, int loopIndex) {
            // Debe tener la estructura: loop ( inicialización; comparación; incremento ) { cuerpo }
            if (loopIndex + 2 < tokens.size()) {
                // Verificar '(' después de 'loop'
                Token afterLoop = tokens.get(loopIndex + 1);
                if (afterLoop.getType() != VGraphLexer.PAR_OPEN) {
                    errorListener.syntaxError(null, afterLoop, afterLoop.getLine(), afterLoop.getCharPositionInLine(),
                            "Se esperaba '(' después de 'loop'", null);
                    return;
                }

                // Buscar los tres componentes del bucle separados por punto y coma
                boolean foundInit = false, foundComparison = false, foundIncrement = false;
                int openParenCount = 1;
                int i = loopIndex + 2;

                while (i < tokens.size() && openParenCount > 0) {
                    Token token = tokens.get(i);

                    if (token.getType() == VGraphLexer.PAR_OPEN) {
                        openParenCount++;
                    } else if (token.getType() == VGraphLexer.PAR_CLOSE) {
                        openParenCount--;
                        if (openParenCount == 0) {
                            // Fin de la declaración del bucle
                            break;
                        }
                    } else if (token.getType() == VGraphLexer.SEMICOLON) {
                        if (!foundInit) {
                            foundInit = true;
                        } else if (!foundComparison) {
                            foundComparison = true;
                        } else {
                            // Ya tenemos los tres componentes
                            foundIncrement = true;
                        }
                    }

                    i++;
                }

                // Verificar si faltan componentes
                if (!foundInit || !foundComparison) {
                    Token loopToken = tokens.get(loopIndex);
                    errorListener.syntaxError(null, loopToken, loopToken.getLine(), loopToken.getCharPositionInLine(),
                            "Estructura incorrecta del bucle. Debe tener: inicialización; comparación; incremento", null);
                }
            }
        }

        // Validación específica para la estructura de condicionales if
        private void validateIfStructure(List<Token> tokens, int ifIndex) {
            // Debe tener la estructura: if ( condición ) { cuerpo }
            if (ifIndex + 2 < tokens.size()) {
                // Verificar '(' después de 'if'
                Token afterIf = tokens.get(ifIndex + 1);
                if (afterIf.getType() != VGraphLexer.PAR_OPEN) {
                    errorListener.syntaxError(null, afterIf, afterIf.getLine(), afterIf.getCharPositionInLine(),
                            "Se esperaba '(' después de 'if'", null);
                    return;
                }

                // Buscar el cierre del paréntesis y luego la apertura de llaves
                boolean foundCloseParen = false;
                boolean foundOpenBrace = false;
                int openParenCount = 1;
                int i = ifIndex + 2;

                while (i < tokens.size() && !foundOpenBrace) {
                    Token token = tokens.get(i);

                    if (token.getType() == VGraphLexer.PAR_OPEN) {
                        openParenCount++;
                    } else if (token.getType() == VGraphLexer.PAR_CLOSE) {
                        openParenCount--;
                        if (openParenCount == 0) {
                            foundCloseParen = true;
                        }
                    } else if (token.getType() == VGraphLexer.BRACKET_OPEN && foundCloseParen) {
                        foundOpenBrace = true;
                        break;
                    }

                    i++;
                }

                // Verificar si falta algún componente
                if (!foundCloseParen || !foundOpenBrace) {
                    Token ifToken = tokens.get(ifIndex);
                    errorListener.syntaxError(null, ifToken, ifToken.getLine(), ifToken.getCharPositionInLine(),
                            "Estructura incorrecta del condicional. Debe tener: if (condición) {cuerpo}", null);
                }
            }
        }

        // Validación específica para asignaciones
        private void validateAssignment(List<Token> tokens, int assignIndex) {
            if (assignIndex > 0 && assignIndex + 1 < tokens.size()) {
                Token prevToken = tokens.get(assignIndex - 1);
                Token nextToken = tokens.get(assignIndex + 1);

                // Verificar si hay un identificador antes del =
                if (prevToken.getType() != VGraphLexer.ID) {
                    errorListener.syntaxError(null, prevToken, prevToken.getLine(), prevToken.getCharPositionInLine(),
                            "Se esperaba un identificador antes de '='", null);
                }

                // Verificar si falta la expresión después del =
                if (nextToken.getType() == VGraphLexer.SEMICOLON ||
                        isOperatorToken(nextToken.getType())) {
                    errorListener.syntaxError(null, nextToken, nextToken.getLine(), nextToken.getCharPositionInLine(),
                            "Asignación incompleta: falta la expresión después de '='", null);
                }
            }
        }

        // Validación de reglas semánticas
        private void validateSemanticRules() {
            // Este método podría implementarse para verificar reglas semánticas adicionales
            // como tipos de variables, uso de variables no declaradas, etc.
        }

        // Verificar la presencia correcta de punto y coma en las sentencias
        private void validateSemicolons() {
            String[] lines = code.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                // Ignorar líneas vacías, comentarios y llaves
                if (line.isEmpty() || line.startsWith("#") ||
                        line.equals("{") || line.equals("}") ||
                        // Ignorar líneas que terminan con apertura de bloque
                        line.endsWith("{")) {
                    continue;
                }

                // Verificar si la línea termina con punto y coma, excepto para estructuras de control
                if (!line.endsWith(";") &&
                        !line.startsWith("if") && !line.startsWith("else") &&
                        !line.startsWith("loop") && !line.startsWith("function")) {

                    // Verificar si es una línea que debe terminar con punto y coma
                    if (line.contains("=") || line.contains("println") ||
                            line.contains("wait") || line.startsWith("(int)") ||
                            line.startsWith("(color)")) {
                        errorListener.syntaxError(null, null, i + 1, line.length(),
                                "Falta punto y coma al final de la sentencia", null);
                    }
                }

                // Verificar asignaciones incompletas (líneas con '=' pero sin nada después)
                if (line.contains("=")) {
                    int equalsPos = line.indexOf('=');
                    if (equalsPos == line.length() - 1 ||
                            (equalsPos < line.length() - 1 && line.substring(equalsPos + 1).trim().isEmpty())) {
                        errorListener.syntaxError(null, null, i + 1, equalsPos,
                                "Asignación incompleta: falta la expresión después de '='", null);
                    }
                }

                // Verificar operaciones incompletas (operador al final de la línea)
                for (char op : new char[]{'+', '-', '*', '/', '%'}) {
                    if (line.endsWith(String.valueOf(op))) {
                        errorListener.syntaxError(null, null, i + 1, line.length() - 1,
                                "Operación incompleta: falta el operando después de '" + op + "'", null);
                    }
                }
            }
        }

        // Verificar si un tipo de token es un operador
        private boolean isOperatorToken(int tokenType) {
            return tokenType == VGraphLexer.PLUS ||
                    tokenType == VGraphLexer.MINUS ||
                    tokenType == VGraphLexer.MULT ||
                    tokenType == VGraphLexer.DIV ||
                    tokenType == VGraphLexer.MODULUS;
        }

        // Contar ocurrencias de un carácter en una cadena
        private int countOccurrences(String str, char c) {
            int count = 0;
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == c) {
                    count++;
                }
            }
            return count;
        }
    }

    public MainIDE() {
        // Set up the main window
        setTitle("VGraph IDE");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize components
        initComponents();

        // Make the window visible
        setVisible(true);
    }

    private void initComponents() {
        // Set up layout
        setLayout(new BorderLayout());

        // Create code editing area with line numbers
        codeArea = new JTextArea();
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumbers = new JTextArea("1");
        lineNumbers.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumbers.setBackground(new Color(240, 240, 240));
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // Highlight current line
        codeArea.setCaretPosition(0);

        // Create document listener to update line numbers
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

        // Create error area
        errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        errorArea.setForeground(Color.RED);
        JScrollPane errorPane = new JScrollPane(errorArea);
        errorPane.setPreferredSize(new Dimension(getWidth(), 150));

        // Set up the code scroll pane with line numbers
        JScrollPane codeScrollPane = new JScrollPane(codeArea);
        codeScrollPane.setRowHeaderView(lineNumbers);

        // Create the toolbar with buttons
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        loadButton = new JButton("Cargar");
        saveButton = new JButton("Guardar");
        compileButton = new JButton("Compilar");
        runButton = new JButton("Ejecutar");

        toolbar.add(loadButton);
        toolbar.add(saveButton);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(compileButton);
        toolbar.add(runButton);

        // Initialize file chooser
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("VGraph Files (." + EXTENSION + ")", EXTENSION));

        // Add components to the frame
        add(toolbar, BorderLayout.NORTH);
        add(codeScrollPane, BorderLayout.CENTER);
        add(errorPane, BorderLayout.SOUTH);

        // Add button action listeners
        loadButton.addActionListener(e -> loadFile());
        saveButton.addActionListener(e -> saveFile());
        compileButton.addActionListener(e -> compileCode());
        runButton.addActionListener(e -> runCode());
    }

    private void updateLineNumbers() {
        try {
            int caretPosition = codeArea.getDocument().getLength();
            Rectangle r = codeArea.modelToView(caretPosition);
            int lineHeight = r.height;
            int lineCount = codeArea.getDocument().getDefaultRootElement().getElementCount();

            StringBuilder numbers = new StringBuilder();
            for (int i = 1; i <= lineCount; i++) {
                numbers.append(i).append("\n");
            }

            if (!lineNumbers.getText().equals(numbers.toString())) {
                lineNumbers.setText(numbers.toString());
            }
        } catch (BadLocationException e) {
            // Ignore exception
        }
    }

    private void loadFile() {
        int returnVal = fileChooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
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
                errorArea.setText("");
                setTitle("VGraph IDE - " + currentFile.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al cargar el archivo: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            int returnVal = fileChooser.showSaveDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
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
            JOptionPane.showMessageDialog(this, "Archivo guardado correctamente.",
                    "Guardar", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void compileCode() {
        errorArea.setText("");

        if (codeArea.getText().trim().isEmpty()) {
            errorArea.setText("No hay código para compilar.");
            return;
        }

        // Crear validador con el código actual
        VGraphErrorListener errorListener = new VGraphErrorListener(errorArea, codeArea.getText());
        AdvancedCodeValidator validator = new AdvancedCodeValidator(codeArea.getText(), errorListener);

        // Limpiar cualquier resaltado anterior
        Highlighter highlighter = codeArea.getHighlighter();
        highlighter.removeAllHighlights();

        // Validar el código
        boolean isValid = validator.validate();

        if (isValid) {
            errorArea.setForeground(new Color(0, 150, 0)); // Verde oscuro
            errorArea.setText("Compilación exitosa. No se encontraron errores.\nAnálisis semántico completado.");
        } else {
            errorArea.setForeground(Color.RED);
            // Resaltar las líneas con errores
            highlightErrorLines(errorListener.getSyntaxErrors());
        }
    }

    private void highlightErrorLines(List<String> errors) {
        Highlighter highlighter = codeArea.getHighlighter();
        highlighter.removeAllHighlights();

        try {
            Highlighter.HighlightPainter errorPainter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200));

            // Extraer números de línea de los mensajes de error
            for (String error : errors) {
                // Buscar patrón "Error en línea X:Y"
                Pattern pattern = Pattern.compile("Error en línea (\\d+):");
                Matcher matcher = pattern.matcher(error);
                if (matcher.find()) {
                    try {
                        int lineNum = Integer.parseInt(matcher.group(1));

                        // Obtener la posición de inicio y fin de la línea
                        if (lineNum > 0 && lineNum <= codeArea.getLineCount()) {
                            int startOffset = codeArea.getLineStartOffset(lineNum - 1);
                            int endOffset = codeArea.getLineEndOffset(lineNum - 1);

                            // Resaltar la línea
                            highlighter.addHighlight(startOffset, endOffset, errorPainter);
                        }
                    } catch (NumberFormatException e) {
                        // Ignorar si no se puede parsear el número de línea
                    }
                }
            }
        } catch (BadLocationException e) {
            // Ignorar excepciones de ubicación
        }
    }

    private void runCode() {
        // Primero compilar para verificar errores
        errorArea.setText("");

        if (codeArea.getText().trim().isEmpty()) {
            errorArea.setText("No hay código para ejecutar.");
            return;
        }

        // Validar el código
        VGraphErrorListener errorListener = new VGraphErrorListener(errorArea, codeArea.getText());
        AdvancedCodeValidator validator = new AdvancedCodeValidator(codeArea.getText(), errorListener);
        boolean isValid = validator.validate();

        if (!isValid) {
            errorArea.setForeground(Color.RED);
            errorArea.append("\nNo se puede ejecutar el código con errores.");
            highlightErrorLines(errorListener.getSyntaxErrors());
            return;
        }

        try {
            // Preparar el analizador léxico
            CharStream in = CharStreams.fromString(codeArea.getText());
            VGraphLexer lexer = new VGraphLexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Preparar el parser
            VGraphParser parser = new VGraphParser(tokens);

            // Ejecutar el parser
            VGraphParser.ProgramContext tree = parser.program();

            // Ejecutar el código a través del visitor
            VGraphCustomVisitor visitor = new VGraphCustomVisitor();
            visitor.visit(tree);

            errorArea.setForeground(new Color(0, 150, 0)); // Verde oscuro
            errorArea.setText("Ejecución iniciada. El código está siendo enviado al dispositivo externo VGA.\n");
            errorArea.append("La visualización se mostrará en el dispositivo externo conectado.");
        } catch (Exception e) {
            errorArea.setForeground(Color.RED);
            errorArea.setText("Error en la ejecución: " + e.getMessage());
            if (e.getCause() != null) {
                errorArea.append("\nCausa: " + e.getCause().getMessage());
            }
            e.printStackTrace();
        }
    }

    // Método principal para lanzar el IDE
    public static void main(String[] args) {
        try {
            // Establecer el look and feel nativo del sistema
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new MainIDE());
    }
}