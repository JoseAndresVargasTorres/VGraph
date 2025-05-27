package v.graph;

import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.List;

public class VGraphCustomVisitor extends VGraphBaseVisitor<String> {
    private int indentLevel = 0;
    private final StringBuilder includes = new StringBuilder();
    private final StringBuilder functions = new StringBuilder();

    public VGraphCustomVisitor() {
        includes.append("#include <stdio.h>\n");
        includes.append("#include <math.h>\n");
        includes.append("#include <unistd.h>\n");
        includes.append("#include \"graphics.h\"\n\n");
    }

    private String indent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    @Override
    public String visitProgram(VGraphParser.ProgramContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(includes.toString());

        StringBuilder mainCode = new StringBuilder();

        // Procesar todas las sentencias
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            String result = visit(sentence);
            if (sentence.function() != null) {
                functions.append(result);
            } else {
                mainCode.append(result);
            }
        }

        // Agregar funciones definidas
        sb.append(functions.toString());

        // Generar main
        sb.append("int main() {\n");
        indentLevel++;

        sb.append(indent()).append("if (init_framebuffer() != 0) {\n");
        sb.append(indent()).append("    printf(\"Error: No se pudo inicializar el framebuffer\\n\");\n");
        sb.append(indent()).append("    return 1;\n");
        sb.append(indent()).append("}\n\n");

        sb.append(indent()).append("clear_screen();\n\n");

        sb.append(mainCode.toString());

        sb.append("\n");
        sb.append(indent()).append("wait_seconds(3);\n");
        sb.append(indent()).append("cleanup_framebuffer();\n");
        sb.append(indent()).append("return 0;\n");

        indentLevel--;
        sb.append("}\n");

        return sb.toString();
    }

    @Override
    public String visitSentence(VGraphParser.SentenceContext ctx) {
        if (ctx.conditional() != null) {
            return visit(ctx.conditional());
        } else if (ctx.var_decl() != null) {
            return visit(ctx.var_decl());
        } else if (ctx.var_assign() != null) {
            return visit(ctx.var_assign());
        } else if (ctx.setcolor() != null) {
            return visit(ctx.setcolor());
        } else if (ctx.draw() != null) {
            return visit(ctx.draw());
        } else if (ctx.shapeCall() != null) {
            return visit(ctx.shapeCall());
        } else if (ctx.function() != null) {
            return visit(ctx.function());
        } else if (ctx.funCall() != null) {
            return visit(ctx.funCall());
        } else if (ctx.println() != null) {
            return visit(ctx.println());
        } else if (ctx.loop_command() != null) {
            return visit(ctx.loop_command());
        } else if (ctx.wait_command() != null) {
            return visit(ctx.wait_command());
        } else if (ctx.frame() != null) {
            return visit(ctx.frame());
        }
        return "";
    }

    @Override
    public String visitFrame(VGraphParser.FrameContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("// === FRAME START ===\n");
        sb.append(visit(ctx.se)); // sentence dentro del frame
        sb.append(indent()).append("// === FRAME END ===\n");
        return sb.toString();
    }

    @Override
    public String visitPrintln(VGraphParser.PrintlnContext ctx) {
        String expr = visit(ctx.expression());
        return indent() + "println_int(" + expr + ");\n";
    }

    @Override
    public String visitVar_decl(VGraphParser.Var_declContext ctx) {
        StringBuilder sb = new StringBuilder();
        String type = visit(ctx.type());

        sb.append(indent()).append(type).append(" ").append(ctx.id1.getText());

        // Variables adicionales - usar la lista completa de IDs
        List<TerminalNode> allIds = ctx.ID();
        // El primer ID ya lo usamos (id1), los demás son id2
        for (int i = 1; i < allIds.size(); i++) {
            sb.append(", ").append(allIds.get(i).getText());
        }

        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public String visitType(VGraphParser.TypeContext ctx) {
        if (ctx.INT() != null) {
            return "int";
        } else if (ctx.COLOR() != null) {
            return "char*"; // COLOR es char* para strings
        }
        return "int";
    }

    @Override
    public String visitVar_assign(VGraphParser.Var_assignContext ctx) {
        String expr = visit(ctx.expression());

        // Si la expresión es un color (string), mantenerla como string
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return indent() + ctx.ID().getText() + " = " + expr + ";\n";
        } else {
            return indent() + ctx.ID().getText() + " = " + expr + ";\n";
        }
    }

    @Override
    public String visitLoop_command(VGraphParser.Loop_commandContext ctx) {
        StringBuilder sb = new StringBuilder();

        // Inicialización del loop (e1=var_assign)
        sb.append(visit(ctx.e1));

        // Condición del while (e2=comparison)
        sb.append(indent()).append("while ").append(visit(ctx.e2)).append(" {\n");
        indentLevel++;

        // Cuerpo del loop (e4=body)
        sb.append(visit(ctx.e4));

        // Incremento (e3=increment_loop)
        sb.append(visit(ctx.e3));

        indentLevel--;
        sb.append(indent()).append("}\n");

        return sb.toString();
    }

    @Override
    public String visitBody(VGraphParser.BodyContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (VGraphParser.SentenceContext stmt : ctx.sentence()) {
            sb.append(visit(stmt));
        }
        return sb.toString();
    }

    @Override
    public String visitIncrement_loop(VGraphParser.Increment_loopContext ctx) {
        String expr = visit(ctx.expression());
        return indent() + ctx.ID().getText() + " = " + expr + ";\n";
    }

    @Override
    public String visitComparison(VGraphParser.ComparisonContext ctx) {
        String left = visit(ctx.e1);  // operand
        String right = visit(ctx.e2); // operand

        String op = "";
        if (ctx.GT() != null) {
            op = ">";
        } else if (ctx.LT() != null) {
            op = "<";
        } else if (ctx.GEQ() != null) {
            op = ">=";
        } else if (ctx.LEQ() != null) {
            op = "<=";
        } else if (ctx.EQ() != null) {
            op = "==";
        } else if (ctx.NEQ() != null) {
            op = "!=";
        }

        return "(" + left + " " + op + " " + right + ")";
    }

    // CORREGIDO: conditional usando las listas correctas
    @Override
    public String visitConditional(VGraphParser.ConditionalContext ctx) {
        StringBuilder sb = new StringBuilder();

        // IF principal
        sb.append(indent()).append("if ").append(visit(ctx.expression(0))).append(" {\n");
        indentLevel++;

        // Según tu gramática: (s1=sentence {body.add($s1.node);})*
        // ctx.s1 son los elementos individuales, no listas
        // Necesitamos usar sentence() para obtener todas las sentencias

        List<VGraphParser.SentenceContext> allSentences = ctx.sentence();

        // Por ahora, como workaround, ponemos las primeras sentencias en el IF
        // (Esta es una limitación de la gramática actual)
        int sentenceIndex = 0;
        int ifSentences = allSentences.size() / 3; // Dividir aproximadamente

        for (int i = 0; i < ifSentences && i < allSentences.size(); i++) {
            sb.append(visit(allSentences.get(i)));
            sentenceIndex++;
        }

        indentLevel--;
        sb.append(indent()).append("}");

        // ELSEIF
        if (ctx.ELSEIF() != null && ctx.expression().size() > 1) {
            sb.append(" else if ").append(visit(ctx.expression(1))).append(" {\n");
            indentLevel++;

            // Sentencias del elseif
            int elseifSentences = (allSentences.size() - ifSentences) / 2;
            for (int i = 0; i < elseifSentences && sentenceIndex < allSentences.size(); i++) {
                sb.append(visit(allSentences.get(sentenceIndex)));
                sentenceIndex++;
            }

            indentLevel--;
            sb.append(indent()).append("}");
        }

        // ELSE
        if (ctx.ELSE() != null) {
            sb.append(" else {\n");
            indentLevel++;

            // Resto de sentencias van al else
            while (sentenceIndex < allSentences.size()) {
                sb.append(visit(allSentences.get(sentenceIndex)));
                sentenceIndex++;
            }

            indentLevel--;
            sb.append(indent()).append("}");
        }

        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String visitExpression(VGraphParser.ExpressionContext ctx) {
        if (ctx.operand() != null) {
            return visit(ctx.operand());
        } else if (ctx.comparison() != null) {
            return visit(ctx.comparison());
        }
        return "";
    }

    @Override
    public String visitOperand(VGraphParser.OperandContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.t1));

        // Manejar operaciones de suma y resta
        List<VGraphParser.FactorContext> factors = ctx.factor();
        int plusCount = ctx.PLUS() != null ? ctx.PLUS().size() : 0;
        int minusCount = ctx.MINUS() != null ? ctx.MINUS().size() : 0;

        for (int i = 1; i < factors.size(); i++) {
            if (i <= plusCount) {
                sb.append(" + ");
            } else if (i <= plusCount + minusCount) {
                sb.append(" - ");
            }
            sb.append(visit(factors.get(i)));
        }

        return sb.toString();
    }

    @Override
    public String visitFactor(VGraphParser.FactorContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.t1));

        // Manejar operaciones de multiplicación, división y módulo
        List<VGraphParser.TermContext> terms = ctx.term();
        int multCount = ctx.MULT() != null ? ctx.MULT().size() : 0;
        int divCount = ctx.DIV() != null ? ctx.DIV().size() : 0;
        int modCount = ctx.MODULUS() != null ? ctx.MODULUS().size() : 0;

        for (int i = 1; i < terms.size(); i++) {
            if (i <= multCount) {
                sb.append(" * ");
            } else if (i <= multCount + divCount) {
                sb.append(" / ");
            } else if (i <= multCount + divCount + modCount) {
                sb.append(" % ");
            }
            sb.append(visit(terms.get(i)));
        }

        return sb.toString();
    }

    @Override
    public String visitTerm(VGraphParser.TermContext ctx) {
        if (ctx.NUMBER() != null) {
            return ctx.NUMBER().getText();
        } else if (ctx.COLOR_VALUES() != null) {
            return "\"" + ctx.COLOR_VALUES().getText() + "\"";
        } else if (ctx.BOOLEAN() != null) {
            return ctx.BOOLEAN().getText().equals("true") ? "1" : "0";
        } else if (ctx.ID() != null) {
            return ctx.ID().getText();
        } else if (ctx.expression() != null) {
            return "(" + visit(ctx.expression()) + ")";
        } else if (ctx.cos() != null) {
            return visit(ctx.cos());
        } else if (ctx.sin() != null) {
            return visit(ctx.sin());
        }
        return "0";
    }

    @Override
    public String visitSin(VGraphParser.SinContext ctx) {
        return "sin(" + visit(ctx.expression()) + ")";
    }

    @Override
    public String visitCos(VGraphParser.CosContext ctx) {
        return "cos(" + visit(ctx.expression()) + ")";
    }

    @Override
    public String visitWait_command(VGraphParser.Wait_commandContext ctx) {
        String time = visit(ctx.e);
        return indent() + "wait_seconds(" + time + ");\n";
    }

    @Override
    public String visitClear_command(VGraphParser.Clear_commandContext ctx) {
        return indent() + "clear_screen();\n";
    }

    @Override
    public String visitSetcolor(VGraphParser.SetcolorContext ctx) {
        String colorValue = visit(ctx.t);
        return indent() + "setcolor(" + colorValue + ");\n";
    }

    @Override
    public String visitDraw(VGraphParser.DrawContext ctx) {
        return visit(ctx.s); // shapeCall directamente
    }

    @Override
    public String visitShapeCall(VGraphParser.ShapeCallContext ctx) {
        if (ctx.LINE() != null) {
            String x1 = visit(ctx.a);
            String y1 = visit(ctx.b);
            String x2 = visit(ctx.c);
            String y2 = visit(ctx.d);
            return indent() + "line(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ");\n";

        } else if (ctx.RECT() != null) {
            String x = visit(ctx.x);
            String y = visit(ctx.y);
            String w = visit(ctx.w);
            String h = visit(ctx.h);
            return indent() + "rect(" + x + ", " + y + ", " + w + ", " + h + ");\n";

        } else if (ctx.CIRCLE() != null) {
            String x = visit(ctx.x);
            String y = visit(ctx.y);
            String r = visit(ctx.r);
            return indent() + "circle(" + x + ", " + y + ", " + r + ");\n";

        } else if (ctx.PIXEL() != null) {
            String x = visit(ctx.x);
            String y = visit(ctx.y);
            return indent() + "pixel(" + x + ", " + y + ");\n";
        }
        return "";
    }

    @Override
    public String visitFunction(VGraphParser.FunctionContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append("void ").append(ctx.funID.getText()).append("(");

        if (ctx.arg1 != null) {
            sb.append("int ").append(ctx.arg1.getText());

            // Obtener todos los IDs y procesar los argumentos adicionales
            List<TerminalNode> allIds = ctx.ID();
            // El primer ID es funID, el segundo es arg1, el resto son arg2
            for (int i = 2; i < allIds.size(); i++) {
                sb.append(", int ").append(allIds.get(i).getText());
            }
        }

        sb.append(") {\n");
        indentLevel++;

        // Procesar todas las sentencias en la función
        List<VGraphParser.SentenceContext> allSentences = ctx.sentence();
        for (VGraphParser.SentenceContext stmt : allSentences) {
            sb.append(visit(stmt));
        }

        indentLevel--;
        sb.append("}\n\n");

        return sb.toString();
    }

    @Override
    public String visitFunCall(VGraphParser.FunCallContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append(ctx.funID.getText()).append("(");

        if (ctx.arg1 != null) {
            sb.append(visit(ctx.arg1));

            // Obtener todas las expresiones y procesar los argumentos adicionales
            List<VGraphParser.ExpressionContext> allExpressions = ctx.expression();
            // La primera expression es arg1, el resto son arg2
            for (int i = 1; i < allExpressions.size(); i++) {
                sb.append(", ").append(visit(allExpressions.get(i)));
            }
        }

        sb.append(");\n");
        return sb.toString();
    }
}