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

            CharStream in = CharStreams.fromFileName("C:\\Users\\Gaby\\Documents\\VII semestre\\Compi\\Proyecto\\VGraph\\VGraph\\src\\test\\resources\\test.vgraph");
            VGraphLexer lexer = new VGraphLexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            VGraphParser parser = new VGraphParser(tokens);
            VGraphParser.ProgramContext tree = parser.program();
            VGraphCustomVisitor visitor = new VGraphCustomVisitor();
            visitor.visit(tree);

            System.out.println("FINISH: " + file);
        }
    }
}
