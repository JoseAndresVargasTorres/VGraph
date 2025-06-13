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

    // MÉTODO saveFile() INTELIGENTE - Exactamente lo que pediste
    private void saveFile() {
        if (currentFile == null) {
            // OPCIÓN 1: Primera vez - directamente al diálogo de guardar
            saveFileAs();
        } else {
            // OPCIÓN 2: Ya existe archivo - mostrar DOS opciones
            showSaveOptionsDialog();
        }
    }

    // MÉTODO: Mostrar diálogo con DOS opciones cuando ya hay archivo guardado
    private void showSaveOptionsDialog() {
        String[] options = {
                "Save Changes",
                "Save As New File"
        };

        String message = "Current file: " + currentFile.getName() + "\n\n" +
                "What would you like to do?";

        int choice = JOptionPane.showOptionDialog(
                this,
                message,
                "Save Options",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0] // "Save Changes" como opción por defecto
        );

        if (choice == 0) {
            // Opción 1: Guardar cambios en el archivo actual
            saveChangesToCurrentFile();
        } else if (choice == 1) {
            // Opción 2: Guardar como archivo nuevo
            saveFileAs();
        }
        // Si presiona X o Cancel, no hace nada
    }

    // MÉTODO: Guardar cambios en el archivo actual
    private void saveChangesToCurrentFile() {
        try {
            FileWriter writer = new FileWriter(currentFile);
            writer.write(codeArea.getText());
            writer.close();

            JOptionPane.showMessageDialog(this,
                    "Changes saved successfully!",
                    "Save Changes",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            showError("Error saving changes: " + e.getMessage());
        }
    }

    // MÉTODO: Guardar como nuevo archivo
    private void saveFileAs() {
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // Asegurar que tenga la extensión correcta
            if (!selectedFile.getName().endsWith("." + EXTENSION)) {
                selectedFile = new File(selectedFile.getAbsolutePath() + "." + EXTENSION);
            }

            try {
                FileWriter writer = new FileWriter(selectedFile);
                writer.write(codeArea.getText());
                writer.close();

                // Actualizar archivo actual
                currentFile = selectedFile;
                setTitle("VGraph IDE - " + currentFile.getName());

                JOptionPane.showMessageDialog(this,
                        "File saved successfully!",
                        "Save",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                showError("Error saving file: " + e.getMessage());
            }
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

    // MÉTODO PRINCIPAL - EJECUCIÓN AUTOMATIZADA EN FPGA - MEJORADO
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
        SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
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
                        return false;
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
                        return false;
                    }
                    publish("✅ File saved to ./" + GENERATED_DIR + "/" + MAIN_C_FILE);

                    // PASO 4: TRANSFERIR A FPGA
                    publish("📤 STEP 4: Transferring to FPGA via SCP...");
                    if (!transferToFPGA()) {
                        publish("❌ Failed to transfer file to FPGA");
                        return false;
                    }
                    publish("✅ Transfer completed successfully!");

                    // PASO 5: COMPILAR EN FPGA
                    publish("🔨 STEP 5: Compiling on FPGA...");
                    if (!compileOnFPGA()) {
                        publish("❌ Compilation failed on FPGA");
                        return false;
                    }
                    publish("✅ Compilation successful on FPGA!");

                    // PASO 6: EJECUTAR EN FPGA
                    publish("🚀 STEP 6: Executing on FPGA...");
                    if (!executeOnFPGA()) {
                        publish("❌ Execution failed on FPGA");
                        return false;
                    }
                    publish("✅ Program started on FPGA!");
                    publish("📺 Monitoring execution output...");
                    publish("📋 Program will automatically finish when completed");
                    publish("⏹️ Or press 'Stop Execution' to terminate manually");

                    return true;

                } catch (Exception e) {
                    publish("❌ ERROR: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    outputArea.append(message + "\n");
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());

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
                try {
                    Boolean success = get();

                    if (!success) {
                        // Hubo un error en la preparación, re-habilitar botones
                        runButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        executionRunning = false;
                        currentFpgaProcess = null;
                    }
                    // Si success == true, el monitoreo automático se encargará de
                    // re-habilitar los botones cuando el proceso termine

                } catch (Exception e) {
                    // Error en el worker, re-habilitar botones
                    runButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    executionRunning = false;
                    currentFpgaProcess = null;
                }
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

    // MÉTODO MEJORADO: executeOnFPGA con monitoreo automático
    private boolean executeOnFPGA() {
        try {
            String host = config.getProperty("fpga.host");
            String user = config.getProperty("fpga.user");
            String remotePath = config.getProperty("fpga.remote.path");

            String command = "cd " + remotePath + " && sudo ./main";

            ProcessBuilder pb = new ProcessBuilder("sshpass", "-p", config.getProperty("fpga.password"),
                    "ssh", user + "@" + host, command);

            currentFpgaProcess = pb.start();

            // NUEVO: Monitorear el proceso en un hilo separado
            monitorProcessExecution();

            return true;
        } catch (Exception e) {
            showError("Error during execution: " + e.getMessage());
            return false;
        }
    }

    // NUEVO MÉTODO: Monitorear la ejecución del proceso
    private void monitorProcessExecution() {
        Thread monitorThread = new Thread(() -> {
            try {
                // Capturar salida del proceso
                BufferedReader outputReader = new BufferedReader(
                        new InputStreamReader(currentFpgaProcess.getInputStream()));
                BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(currentFpgaProcess.getErrorStream()));

                // Leer salida en tiempo real
                SwingWorker<Void, String> outputWorker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        String line;

                        // Leer stdout
                        while ((line = outputReader.readLine()) != null && !isCancelled()) {
                            publish("📺 OUTPUT: " + line);
                        }

                        // Leer stderr si hay errores
                        while ((line = errorReader.readLine()) != null && !isCancelled()) {
                            publish("⚠️ ERROR: " + line);
                        }

                        return null;
                    }

                    @Override
                    protected void process(java.util.List<String> chunks) {
                        for (String message : chunks) {
                            SwingUtilities.invokeLater(() -> {
                                outputArea.append(message + "\n");
                                outputArea.setCaretPosition(outputArea.getDocument().getLength());

                                if (message.contains("ERROR")) {
                                    outputArea.setForeground(Color.RED);
                                } else {
                                    outputArea.setForeground(Color.BLACK);
                                }
                            });
                        }
                    }
                };

                outputWorker.execute();

                // Esperar a que termine el proceso
                int exitCode = currentFpgaProcess.waitFor();

                // Cancelar el worker de salida
                outputWorker.cancel(true);

                // Actualizar interfaz en el hilo de Swing
                SwingUtilities.invokeLater(() -> {
                    handleProcessCompletion(exitCode);
                });

            } catch (InterruptedException e) {
                // El proceso fue interrumpido (probablemente por Stop)
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("⚠️ Process was interrupted\n");
                    outputArea.setForeground(new Color(255, 140, 0));
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("❌ Error monitoring process: " + e.getMessage() + "\n");
                    outputArea.setForeground(Color.RED);
                    handleProcessCompletion(-1);
                });
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    // NUEVO MÉTODO: Manejar la finalización del proceso
    private void handleProcessCompletion(int exitCode) {
        executionRunning = false;
        currentFpgaProcess = null;

        // Re-habilitar botones
        runButton.setEnabled(true);
        stopButton.setEnabled(false);

        // Mostrar mensaje de finalización
        outputArea.append("\n" + "=".repeat(50) + "\n");

        if (exitCode == 0) {
            outputArea.append("✅ PROGRAM COMPLETED SUCCESSFULLY\n");
            outputArea.append("📊 Exit code: " + exitCode + "\n");
            outputArea.append("🎯 Execution finished normally\n");
            outputArea.setForeground(new Color(0, 150, 0));
        } else if (exitCode == -1) {
            outputArea.append("❌ PROGRAM EXECUTION ERROR\n");
            outputArea.append("🔧 Check your VGraph code for runtime errors\n");
            outputArea.setForeground(Color.RED);
        } else {
            outputArea.append("⚠️ PROGRAM FINISHED WITH WARNINGS\n");
            outputArea.append("📊 Exit code: " + exitCode + "\n");
            outputArea.append("💡 Program terminated but may have encountered issues\n");
            outputArea.setForeground(new Color(255, 140, 0));
        }

        outputArea.append("🔄 Ready for next execution - Press 'Run on FPGA' again\n");
        outputArea.append("=".repeat(50) + "\n");
    }

    // MÉTODO stopExecution SIMPLIFICADO
    private void stopExecution() {
        if (!executionRunning) {
            showError("No execution in progress.");
            return;
        }

        outputArea.append("\n🛑 Stopping execution...\n");
        outputArea.setForeground(new Color(255, 140, 0));

        boolean stopped = false;

        // ESTRATEGIA 1: Matar proceso local
        if (currentFpgaProcess != null && currentFpgaProcess.isAlive()) {
            outputArea.append("🔸 Terminating SSH connection...\n");
            currentFpgaProcess.destroyForcibly();

            try {
                boolean terminated = currentFpgaProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                if (terminated) {
                    outputArea.append("✅ Local process terminated\n");
                    stopped = true;
                }
            } catch (InterruptedException e) {
                outputArea.append("⚠️ Interrupted while waiting for process\n");
            }
        }

        // ESTRATEGIA 2: Matar procesos remotos
        try {
            String host = config.getProperty("fpga.host");
            String user = config.getProperty("fpga.user");
            String password = config.getProperty("fpga.password");

            outputArea.append("🔸 Sending kill signal to FPGA...\n");

            String killCommand = "sudo pkill -TERM main; sudo pkill -KILL main";

            ProcessBuilder pb = new ProcessBuilder("sshpass", "-p", password,
                    "ssh", "-o", "ConnectTimeout=5", user + "@" + host, killCommand);

            Process killProcess = pb.start();
            boolean finished = killProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (finished && killProcess.exitValue() == 0) {
                outputArea.append("✅ Remote process terminated\n");
                stopped = true;
            }

        } catch (Exception e) {
            outputArea.append("❌ Error during remote kill: " + e.getMessage() + "\n");
        }

        // Actualizar estado
        outputArea.append("\n🏁 Manual stop completed\n");
        if (stopped) {
            outputArea.append("✅ Execution successfully terminated\n");
            outputArea.setForeground(new Color(0, 150, 0));
        } else {
            outputArea.append("⚠️ Process may still be running on FPGA\n");
            outputArea.setForeground(new Color(255, 140, 0));
        }

        // Re-habilitar botones
        runButton.setEnabled(true);
        stopButton.setEnabled(false);
        executionRunning = false;
        currentFpgaProcess = null;
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