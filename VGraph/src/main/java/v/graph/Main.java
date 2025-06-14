package v.graph;

import java.io.IOException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Main {

    private static final String EXTENSION = "vgraph";
    private static final String DIRBASE = "src/test/resources/";

    public static void main(String[] args) throws IOException {
        String files[] = args.length==0? new String[]{ "test." + EXTENSION } : args;
        System.out.println("Dirbase: " + DIRBASE);
        for (String file : files){
            System.out.println("START: " + file);

            String projectRoot = System.getProperty("user.dir");
            //String fullPath = projectRoot + "/VGraph/" + DIRBASE + file;
            CharStream in = CharStreams.fromFileName("C:\\Users\\josev\\OneDrive\\Documentos\\Semestre II 2025\\Compiladores e Intérpretes\\Proyecto\\VGraph\\VGraph\\src\\test\\resources\\test.vgraph");
            VGraphLexer lexer = new VGraphLexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            VGraphParser parser = new VGraphParser(tokens);
            VGraphParser.ProgramContext tree = parser.program();



            VGraphCustomVisitor visitor = new VGraphCustomVisitor();
            String output   = visitor.visit(tree);

            System.out.println(output);
        }
    }
}
