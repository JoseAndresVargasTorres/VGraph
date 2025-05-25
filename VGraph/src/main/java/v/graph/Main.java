package v.graph;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Main {

    private static final String EXTENSION = "vgraph";
    private static final String DIRBASE = "src/test/resources/";
    private static final String FILE_OUT = "src/main/";

    public static void main(String[] args) throws IOException {
        String files[] = args.length == 0 ? new String[]{"test." + EXTENSION} : args;
        System.out.println("Dirbase: " + DIRBASE);

        for (String file : files) {
            System.out.println("START: " + file);
            Path path = Paths.get("").toAbsolutePath();
            System.out.println("Ruta absoluta del proyecto: " + path);


            CharStream in = CharStreams.fromFileName(path + "/VGraph/" + DIRBASE + file);
            VGraphLexer lexer = new VGraphLexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            VGraphParser parser = new VGraphParser(tokens);


            VGraphParser.ProgramContext tree = parser.program();
            VGraphCustomVisitor visitor = new VGraphCustomVisitor();
            String OUT_C = visitor.visit(tree);
            
            System.out.println(OUT_C);

            String fileNameWithoutExtension = file.substring(0, file.lastIndexOf('.'));
            Path outputPath = Paths.get(FILE_OUT + fileNameWithoutExtension + ".c");
            Files.write(outputPath, OUT_C.getBytes(StandardCharsets.UTF_8));


            System.out.println("Archivo generado: " + outputPath.toAbsolutePath());
            System.out.println("FINISH: " + file);
        }
    }
}
