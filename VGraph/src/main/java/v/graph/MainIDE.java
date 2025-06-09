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
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

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
    private JButton stopButton;
    private JFileChooser fileChooser;
    private File currentFile;
    private static final String EXTENSION = "vgraph";

    // Configuración FPGA
    private static final String CONFIG_FILE = "config.properties";
    private static final String GENERATED_DIR = "generated";
    private static final String MAIN_C_FILE = "main.c";
    private Properties config;
    private Process currentFpgaProcess = null;
    private boolean executionRunning = false;

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
            String errorMsg = "Error at line " + line + ":" + charPositionInLine + " - " + msg;
            errors.add(errorMsg);
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
        setTitle("VGraph IDE - FPGA Compiler");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeConfig();
        initComponents();
        setVisible(true);
    }

    private void initializeConfig() {
        config = new Properties();

        // Crear archivo de configuración si no existe
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            createDefaultConfig();
        }

        // Cargar configuración
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            config.load(fis);
        } catch (IOException e) {
            showError("Error loading configuration: " + e.getMessage());
            createDefaultConfig();
        }
    }

    private void createDefaultConfig() {
        config.setProperty("fpga.host", "172.16.1.2");
        config.setProperty("fpga.user", "root");
        config.setProperty("fpga.password", "temppwd");
        config.setProperty("fpga.port", "22");
        config.setProperty("fpga.remote.path", "/home/ubuntu/vgraph");
        config.setProperty("compile.timeout", "30");

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            config.store(fos, "VGraph FPGA Configuration");
        } catch (IOException e) {
            showError("Error creating configuration file: " + e.getMessage());
        }
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
        runButton = new JButton("Run on FPGA");
        stopButton = new JButton("Stop Execution");
        stopButton.setEnabled(false);
        stopButton.setBackground(new Color(220, 53, 69));
        stopButton.setForeground(Color.WHITE);

        toolbar.add(loadButton);
        toolbar.add(saveButton);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(compileButton);
        toolbar.add(runButton);
        toolbar.add(stopButton);

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
        stopButton.addActionListener(e -> stopExecution());
    }

    private void setupUndoRedo() {
        UndoManager undoManager = new UndoManager();
        Document document = codeArea.getDocument();
        document.addUndoableEditListener(undoManager);

        InputMap inputMap = codeArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = codeArea.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });

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
                showError("Error loading file: " + e.getMessage());
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
            showError("Error saving file: " + e.getMessage());
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
            clearHighlights();
            List<String> allErrors = new ArrayList<>();

            CharStream input = CharStreams.fromString(codeArea.getText());
            VGraphLexer lexer = new VGraphLexer(input);
            SimpleErrorListener lexerErrorListener = new SimpleErrorListener(null);
            lexer.removeErrorListeners();
            lexer.addErrorListener(lexerErrorListener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            VGraphParser parser = new VGraphParser(tokens);
            SimpleErrorListener parserErrorListener = new SimpleErrorListener(null);
            parser.removeErrorListeners();
            parser.addErrorListener(parserErrorListener);

            VGraphParser.ProgramContext tree = null;
            try {
                tree = parser.program();
            } catch (Exception e) {
                // Continúa aunque falle el parsing para mostrar errores
            }

            allErrors.addAll(lexerErrorListener.getErrors());
            allErrors.addAll(parserErrorListener.getErrors());

            if (tree != null) {
                try {
                    SemanticValidator semanticValidator = new SemanticValidator();
                    semanticValidator.visit(tree);
                    allErrors.addAll(semanticValidator.getSemanticErrors());
                } catch (Exception e) {
                    allErrors.add("Error at line 1 - Semantic analysis failed due to syntax errors");
                }
            }

            if (!allErrors.isEmpty()) {
                outputArea.setForeground(Color.RED);
                outputArea.setText("COMPILATION ERRORS FOUND (" + allErrors.size() + "):\n\n");

                for (int i = 0; i < allErrors.size(); i++) {
                    outputArea.append((i + 1) + ". " + allErrors.get(i) + "\n");
                }

                highlightAllErrorLines(allErrors);
            } else {
                outputArea.setForeground(new Color(0, 150, 0));
                outputArea.setText("✅ COMPILATION SUCCESSFUL\n\n");
                outputArea.append("Code compiled successfully with no errors.\n");
                outputArea.append("Press 'Run on FPGA' to execute on hardware.\n");
            }

        } catch (Exception e) {
            outputArea.setForeground(Color.RED);
            outputArea.setText("COMPILATION ERROR:\n\n" + e.getMessage());
        }
    }

    // MÉTODO PRINCIPAL - EJECUCIÓN AUTOMATIZADA EN FPGA
    private void executeCode() {
        if (executionRunning) {
            showError("Execution already in progress. Stop current execution first.");
            return;
        }

        outputArea.setText("");
        outputArea.setForeground(Color.BLACK);

        if (codeArea.getText().trim().isEmpty()) {
            outputArea.setForeground(Color.RED);
            outputArea.setText("No code to execute.");
            return;
        }

        // Deshabilitar botón Run y habilitar Stop
        runButton.setEnabled(false);
        stopButton.setEnabled(true);
        executionRunning = true;

        // Ejecutar en hilo separado para no bloquear la interfaz
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // PASO 1: VALIDAR CÓDIGO
                    publish("🔍 STEP 1: Validating VGraph code...");

                    clearHighlights();
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

                    List<String> allErrors = new ArrayList<>();
                    allErrors.addAll(errorListener.getErrors());

                    if (allErrors.isEmpty()) {
                        SemanticValidator semanticValidator = new SemanticValidator();
                        semanticValidator.visit(tree);
                        allErrors.addAll(semanticValidator.getSemanticErrors());
                    }

                    if (!allErrors.isEmpty()) {
                        publish("❌ VALIDATION FAILED");
                        for (String error : allErrors) {
                            publish("• " + error);
                        }
                        return null;
                    }

                    publish("✅ Code validation successful!");

                    // PASO 2: GENERAR CÓDIGO C
                    publish("🔄 STEP 2: Generating C code...");
                    VGraphCustomVisitor codeGenerator = new VGraphCustomVisitor();
                    String generatedCCode = codeGenerator.visit(tree);
                    publish("✅ C code generated successfully!");

                    // PASO 3: GUARDAR ARCHIVO MAIN.C
                    publish("📁 STEP 3: Saving main.c file...");
                    if (!saveMainCFile(generatedCCode)) {
                        publish("❌ Failed to save main.c file");
                        return null;
                    }
                    publish("✅ File saved to ./" + GENERATED_DIR + "/" + MAIN_C_FILE);

                    // PASO 4: TRANSFERIR A FPGA
                    publish("📤 STEP 4: Transferring to FPGA via SCP...");
                    if (!transferToFPGA()) {
                        publish("❌ Failed to transfer file to FPGA");
                        return null;
                    }
                    publish("✅ Transfer completed successfully!");

                    // PASO 5: COMPILAR EN FPGA
                    publish("🔨 STEP 5: Compiling on FPGA...");
                    if (!compileOnFPGA()) {
                        publish("❌ Compilation failed on FPGA");
                        return null;
                    }
                    publish("✅ Compilation successful on FPGA!");

                    // PASO 6: EJECUTAR EN FPGA
                    publish("🚀 STEP 6: Executing on FPGA...");
                    if (!executeOnFPGA()) {
                        publish("❌ Execution failed on FPGA");
                        return null;
                    }
                    publish("✅ Program running on FPGA!");
                    publish("📺 Check FPGA display for visual output");
                    publish("⏹️ Press 'Stop Execution' to terminate");

                } catch (Exception e) {
                    publish("❌ ERROR: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    outputArea.append(message + "\n");
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());

                    // Colorear mensajes
                    if (message.contains("❌")) {
                        outputArea.setForeground(Color.RED);
                    } else if (message.contains("✅")) {
                        outputArea.setForeground(new Color(0, 150, 0));
                    } else if (message.contains("🔍") || message.contains("🔄") ||
                            message.contains("📁") || message.contains("📤") ||
                            message.contains("🔨") || message.contains("🚀")) {
                        outputArea.setForeground(new Color(0, 100, 200));
                    } else {
                        outputArea.setForeground(Color.BLACK);
                    }
                }
            }

            @Override
            protected void done() {
                // Re-habilitar botones
                runButton.setEnabled(true);
                stopButton.setEnabled(false);
                executionRunning = false;
            }
        };

        worker.execute();
    }

    private boolean saveMainCFile(String cCode) {
        try {
            // Crear directorio generated si no existe
            Path generatedDir = Paths.get(GENERATED_DIR);
            if (!Files.exists(generatedDir)) {
                Files.createDirectories(generatedDir);
            }

            // Guardar archivo main.c
            Path mainCPath = generatedDir.resolve(MAIN_C_FILE);
            Files.write(mainCPath, cCode.getBytes());

            return true;
        } catch (IOException e) {
            showError("Error saving main.c: " + e.getMessage());
            return false;
        }
    }

    private boolean transferToFPGA() {
        try {
            String host = config.getProperty("fpga.host");
            String user = config.getProperty("fpga.user");
            String remotePath = config.getProperty("fpga.remote.path");

            String localFile = GENERATED_DIR + "/" + MAIN_C_FILE;
            String remoteFile = user + "@" + host + ":" + remotePath + "/" + MAIN_C_FILE;

            ProcessBuilder pb = new ProcessBuilder("sshpass", "-p", config.getProperty("fpga.password"),
                    "scp", localFile, remoteFile);

            Process process = pb.start();
            int exitCode = process.waitFor();

            return exitCode == 0;
        } catch (Exception e) {
            showError("Error during SCP transfer: " + e.getMessage());
            return false;
        }
    }

    private boolean compileOnFPGA() {
        try {
            String host = config.getProperty("fpga.host");
            String user = config.getProperty("fpga.user");
            String remotePath = config.getProperty("fpga.remote.path");

            String command = "cd " + remotePath + " && gcc main.c graphics.c -o main -lm";

            ProcessBuilder pb = new ProcessBuilder("sshpass", "-p", config.getProperty("fpga.password"),
                    "ssh", user + "@" + host, command);

            Process process = pb.start();
            int exitCode = process.waitFor();

            return exitCode == 0;
        } catch (Exception e) {
            showError("Error during compilation: " + e.getMessage());
            return false;
        }
    }

    private boolean executeOnFPGA() {
        try {
            String host = config.getProperty("fpga.host");
            String user = config.getProperty("fpga.user");
            String remotePath = config.getProperty("fpga.remote.path");

            String command = "cd " + remotePath + " && sudo ./main";

            ProcessBuilder pb = new ProcessBuilder("sshpass", "-p", config.getProperty("fpga.password"),
                    "ssh", user + "@" + host, command);

            currentFpgaProcess = pb.start();

            // No esperamos a que termine porque puede ejecutar indefinidamente
            return true;
        } catch (Exception e) {
            showError("Error during execution: " + e.getMessage());
            return false;
        }
    }

    private void stopExecution() {
        if (currentFpgaProcess != null && currentFpgaProcess.isAlive()) {
            currentFpgaProcess.destroyForcibly();
            outputArea.append("\n🛑 Execution stopped by user\n");
            outputArea.setForeground(new Color(255, 140, 0));
        }

        runButton.setEnabled(true);
        stopButton.setEnabled(false);
        executionRunning = false;
    }

    private void highlightAllErrorLines(List<String> errors) {
        Highlighter highlighter = codeArea.getHighlighter();

        try {
            Highlighter.HighlightPainter painter =
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200));

            for (String error : errors) {
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

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
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