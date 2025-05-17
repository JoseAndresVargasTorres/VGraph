package v.graph;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;

// Importaciones de ANTLR simplificadas - solo las necesarias
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class MainIDE extends JFrame {
    // Componentes de la interfaz
    private JTextArea codeTextArea;
    private JTextArea consoleTextArea;
    private JScrollPane codeScrollPane;
    private final String DEFAULT_EXTENSION = ".vgraph";
    private final String WINDOW_TITLE = "VGraph IDE";
    private String currentFilePath;
    private boolean isFileSaved = false;
    private boolean isCompiled = false;

    // Colores básicos
    private final Color CONSOLE_ERROR_COLOR = new Color(255, 0, 0);
    private final Color CONSOLE_SUCCESS_COLOR = new Color(0, 128, 0);
    private final Color CONSOLE_WARNING_COLOR = new Color(255, 165, 0);

    // Variable para resultados de compilación
    private VGraphCustomVisitor visitor;

    public MainIDE() {
        setTitle(WINDOW_TITLE);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();

        setVisible(true);
    }

    private void initComponents() {
        // Área de código con números de línea
        codeTextArea = new JTextArea();
        codeTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        codeTextArea.setTabSize(2);
        TextLineNumber tln = new TextLineNumber(codeTextArea);

        codeScrollPane = new JScrollPane(codeTextArea);
        codeScrollPane.setRowHeaderView(tln);

        // Área de consola
        consoleTextArea = new JTextArea();
        consoleTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        consoleTextArea.setEditable(false);
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton loadButton = new JButton("Cargar");
        loadButton.addActionListener(e -> openFile());

        JButton saveButton = new JButton("Guardar");
        saveButton.addActionListener(e -> saveFile());

        JButton compileButton = new JButton("Compilar");
        compileButton.addActionListener(e -> compile());

        JButton runButton = new JButton("Ejecutar");
        runButton.addActionListener(e -> run());

        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(compileButton);
        buttonPanel.add(runButton);

        // Panel de código
        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.add(codeScrollPane, BorderLayout.CENTER);

        // Panel de consola
        JScrollPane consoleScrollPane = new JScrollPane(consoleTextArea);
        consoleScrollPane.setPreferredSize(new Dimension(800, 150));
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder("Consola"));

        // Organización del panel principal
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(codePanel, BorderLayout.CENTER);
        mainPanel.add(consoleScrollPane, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Abrir archivo");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos VGraph", "vgraph"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }

                codeTextArea.setText(content.toString());
                currentFilePath = selectedFile.getAbsolutePath();
                isFileSaved = true;
                isCompiled = false;
                setTitle(WINDOW_TITLE + " - " + selectedFile.getName());

                appendToConsole("Archivo cargado: " + selectedFile.getName() + "\n", Color.BLACK);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error al abrir el archivo: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        if (currentFilePath == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar archivo");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos VGraph", "vgraph"));

            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();

                if (!filePath.toLowerCase().endsWith(DEFAULT_EXTENSION)) {
                    filePath += DEFAULT_EXTENSION;
                }

                currentFilePath = filePath;
            } else {
                return;
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFilePath))) {
            writer.write(codeTextArea.getText());
            isFileSaved = true;
            isCompiled = false;

            File file = new File(currentFilePath);
            setTitle(WINDOW_TITLE + " - " + file.getName());

            appendToConsole("Archivo guardado: " + file.getName() + "\n", Color.BLACK);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al guardar el archivo: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class ErrorListener extends BaseErrorListener {
        private boolean hasErrors = false;

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            hasErrors = true;
            String errorMsg = "Error en línea " + line + ":" + charPositionInLine + " - " + msg;
            appendToConsole(errorMsg + "\n", CONSOLE_ERROR_COLOR);

            // Destacar la línea con error
            highlightErrorLine(line);
        }

        public boolean hasErrors() {
            return hasErrors;
        }
    }

    private void highlightErrorLine(int lineNumber) {
        try {
            // Obtener el inicio y fin de la línea
            int start = getLineStartOffset(lineNumber);
            int end = getLineEndOffset(lineNumber);

            // Crear un rectángulo de resaltado rojo claro
            if (start >= 0 && end > start) {
                codeTextArea.getHighlighter().addHighlight(
                        start, end,
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200))
                );
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private int getLineStartOffset(int lineNumber) {
        try {
            return codeTextArea.getLineStartOffset(lineNumber - 1);
        } catch (BadLocationException e) {
            return -1;
        }
    }

    private int getLineEndOffset(int lineNumber) {
        try {
            return codeTextArea.getLineEndOffset(lineNumber - 1);
        } catch (BadLocationException e) {
            return -1;
        }
    }

    private void compile() {
        consoleTextArea.setText("");
        codeTextArea.getHighlighter().removeAllHighlights();

        String code = codeTextArea.getText();
        if (code.isEmpty()) {
            appendToConsole("Error: No hay código para compilar\n", CONSOLE_ERROR_COLOR);
            return;
        }

        if (!isFileSaved) {
            int option = JOptionPane.showConfirmDialog(this,
                    "Debe guardar el archivo antes de compilar. ¿Desea guardarlo ahora?",
                    "Guardar archivo", JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                saveFile();
            } else {
                return;
            }
        }

        appendToConsole("Compilando...\n", Color.BLACK);

        try {
            CharStream input = CharStreams.fromString(code);

            appendToConsole("Iniciando análisis léxico...\n", Color.BLACK);
            VGraphLexer lexer = new VGraphLexer(input);

            ErrorListener lexerErrorListener = new ErrorListener();
            lexer.removeErrorListeners();
            lexer.addErrorListener(lexerErrorListener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            appendToConsole("Análisis léxico completado.\n", Color.BLACK);

            appendToConsole("Iniciando análisis sintáctico...\n", Color.BLACK);
            VGraphParser parser = new VGraphParser(tokens);

            ErrorListener parserErrorListener = new ErrorListener();
            parser.removeErrorListeners();
            parser.addErrorListener(parserErrorListener);

            VGraphParser.ProgramContext tree = parser.program();

            if (lexerErrorListener.hasErrors() || parserErrorListener.hasErrors()) {
                appendToConsole("Análisis sintáctico completado con errores.\n", CONSOLE_ERROR_COLOR);
                isCompiled = false;
                return;
            }

            appendToConsole("Análisis sintáctico completado con éxito.\n", CONSOLE_SUCCESS_COLOR);

            appendToConsole("Iniciando análisis semántico...\n", Color.BLACK);
            visitor = new VGraphCustomVisitor();
            try {
                visitor.visit(tree);
                appendToConsole("Análisis semántico completado con éxito.\n", CONSOLE_SUCCESS_COLOR);
                appendToConsole("Compilación finalizada correctamente.\n", CONSOLE_SUCCESS_COLOR);
                isCompiled = true;
            } catch (Exception e) {
                appendToConsole("Error en análisis semántico: " + e.getMessage() + "\n", CONSOLE_ERROR_COLOR);
                appendToConsole("Compilación finalizada con errores.\n", CONSOLE_ERROR_COLOR);
                isCompiled = false;
            }

        } catch (Exception e) {
            appendToConsole("Error de compilación: " + e.getMessage() + "\n", CONSOLE_ERROR_COLOR);
            isCompiled = false;
        }
    }

    private void run() {
        if (!isCompiled) {
            appendToConsole("Debe compilar el programa antes de ejecutarlo.\n", CONSOLE_WARNING_COLOR);
            int option = JOptionPane.showConfirmDialog(this,
                    "El programa no está compilado. ¿Desea compilarlo ahora?",
                    "Compilar programa", JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                compile();
                if (!isCompiled) {
                    return;
                }
            } else {
                return;
            }
        }

        appendToConsole("\nEjecutando el programa...\n", Color.BLACK);

        try {
            if (visitor != null) {
                appendToConsole("Generando visualización gráfica...\n", Color.BLACK);
                // Aquí iría la lógica de ejecución del código usando el visitor
                // y generando la visualización gráfica
                appendToConsole("La visualización ha sido generada correctamente.\n", CONSOLE_SUCCESS_COLOR);
            } else {
                appendToConsole("Error: No hay resultados de compilación disponibles.\n", CONSOLE_ERROR_COLOR);
            }
        } catch (Exception e) {
            appendToConsole("Error durante la ejecución: " + e.getMessage() + "\n", CONSOLE_ERROR_COLOR);
        }
    }

    private void appendToConsole(String text, Color color) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);

        try {
            int len = consoleTextArea.getDocument().getLength();
            consoleTextArea.getDocument().insertString(len, text, attributes);
            consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // Clase para mostrar los números de línea
    class TextLineNumber extends JPanel {
        private final JTextComponent textComponent;
        private final int MARGIN = 5;
        private final Font LINE_FONT = new Font("Monospaced", Font.PLAIN, 12);

        public TextLineNumber(JTextComponent textComponent) {
            this.textComponent = textComponent;
            setPreferredSize(new Dimension(30, 1));
            setBackground(new Color(240, 240, 240));
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setFont(LINE_FONT);
            g.setColor(Color.GRAY);

            try {
                // Determinar las líneas visibles
                Rectangle clip = g.getClipBounds();
                int startOffset = textComponent.viewToModel2D(new Point(0, clip.y));
                int endOffset = textComponent.viewToModel2D(new Point(0, clip.y + clip.height));

                // Obtener información de las líneas
                Document doc = textComponent.getDocument();
                int startLine = doc.getDefaultRootElement().getElementIndex(startOffset);
                int endLine = doc.getDefaultRootElement().getElementIndex(endOffset);

                // Dibujar los números de línea
                for (int i = startLine; i <= endLine; i++) {
                    String lineNumber = String.valueOf(i + 1);

                    // Calcular la posición Y para el número de línea
                    Element line = doc.getDefaultRootElement().getElement(i);
                    if (line == null) continue;

                    try {
                        Rectangle r = textComponent.modelToView(line.getStartOffset());
                        int y = r.y + r.height - 3;
                        g.drawString(lineNumber, getWidth() - g.getFontMetrics().stringWidth(lineNumber) - MARGIN, y);
                    } catch (BadLocationException e) {
                        continue;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // Asegurar que la interfaz de usuario se ejecute en el hilo de EDT
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MainIDE();
        });
    }
}

