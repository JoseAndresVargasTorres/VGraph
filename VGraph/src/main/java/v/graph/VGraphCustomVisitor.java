package v.graph;

import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class VGraphCustomVisitor extends VGraphBaseVisitor<String> {
    private int indentLevel = 0;
    private final StringBuilder includes = new StringBuilder();
    private final StringBuilder globalVars = new StringBuilder();
    private final StringBuilder functions = new StringBuilder();

    // NUEVO: Mapa para rastrear tipos de variables
    private final Map<String, String> variableTypes = new HashMap<>();

    // Variables conflictivas con math.h que necesitan ser renombradas
    private static final Set<String> MATH_CONFLICTS = new HashSet<>();
    static {
        MATH_CONFLICTS.add("y1");   // Función de Bessel
        MATH_CONFLICTS.add("y0");   // Función de Bessel
        MATH_CONFLICTS.add("j0");   // Función de Bessel
        MATH_CONFLICTS.add("j1");   // Función de Bessel
        MATH_CONFLICTS.add("gamma"); // Función gamma
        MATH_CONFLICTS.add("exp");   // Función exponencial
        MATH_CONFLICTS.add("log");   // Función logaritmo
        MATH_CONFLICTS.add("sin");   // Función seno
        MATH_CONFLICTS.add("cos");   // Función coseno
        MATH_CONFLICTS.add("tan");   // Función tangente
    }

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

    // Función para resolver conflictos de nombres
    private String resolveVariableName(String varName) {
        if (MATH_CONFLICTS.contains(varName)) {
            return "var_" + varName;  // Prefijo para evitar conflictos
        }
        return varName;
    }

    // MODIFICADO: Método para verificar si una expresión es constante
    private boolean isConstantExpression(String expr) {
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return true; // Es un string (color)
        }

        // Verificar si es un número simple (entero o decimal)
        try {
            Double.parseDouble(expr);
            return true;
        } catch (NumberFormatException e) {
            // No es un número simple
        }

        // Verificar si contiene funciones matemáticas o variables
        if (expr.contains("cos(") || expr.contains("sin(") ||
                expr.contains("+") || expr.contains("-") || expr.contains("*") || expr.contains("/") ||
                expr.matches(".*[a-zA-Z_][a-zA-Z0-9_]*.*")) {
            return false; // Contiene expresiones complejas
        }

        return true;
    }

    @Override
    public String visitProgram(VGraphParser.ProgramContext ctx) {
        StringBuilder sb = new StringBuilder();
        StringBuilder mainCode = new StringBuilder();
        StringBuilder mainInitializations = new StringBuilder();

        // PRIMERA PASADA: Recopilar declaraciones de variables globales
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            if (sentence.var_decl() != null) {
                VGraphParser.Var_declContext varDeclCtx = sentence.var_decl();

                if (varDeclCtx.expression() != null) {
                    // Declaración con asignación: verificar si es constante
                    String expr = visit(varDeclCtx.expression());
                    String varName = resolveVariableName(varDeclCtx.id1.getText());
                    String type = visit(varDeclCtx.type());

                    // NUEVO: Registrar el tipo de la variable
                    variableTypes.put(varName, type);

                    if (isConstantExpression(expr)) {
                        // Es constante, puede ir como global
                        String globalVar = visit(varDeclCtx);
                        globalVars.append(globalVar);
                    } else {
                        // No es constante, declarar como global sin inicializar
                        globalVars.append(type).append(" ").append(varName).append(";\n");

                        // MODIFICADO: Solo convertir a entero si el tipo es int
                        if (type.equals("int") && expr.contains(".")) {
                            try {
                                double doubleValue = Double.parseDouble(expr);
                                expr = String.valueOf((int) doubleValue);
                            } catch (NumberFormatException e) {
                                // Si no es un número directo, mantener la expresión
                            }
                        }
                        // Si es double, mantener el valor decimal

                        mainInitializations.append("    ").append(varName).append(" = ").append(expr).append(";\n");
                    }
                } else {
                    // Declaración simple sin asignación
                    String globalVar = visit(varDeclCtx);
                    globalVars.append(globalVar);

                    // NUEVO: Registrar tipos de variables múltiples
                    String type = visit(varDeclCtx.type());
                    String firstVar = resolveVariableName(varDeclCtx.id1.getText());
                    variableTypes.put(firstVar, type);

                    List<TerminalNode> allIds = varDeclCtx.ID();
                    for (int i = 1; i < allIds.size(); i++) {
                        String varName = resolveVariableName(allIds.get(i).getText());
                        variableTypes.put(varName, type);
                    }
                }
            }
        }

        // SEGUNDA PASADA: Procesar funciones y el resto del código
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            String result = visit(sentence);
            if (sentence.function() != null) {
                functions.append(result);
            } else if (sentence.var_decl() == null) {
                mainCode.append(result);
            }
        }

        // CONSTRUIR EL ARCHIVO FINAL
        sb.append(includes.toString());

        if (globalVars.length() > 0) {
            sb.append("// Global variables\n");
            sb.append(globalVars.toString());
            sb.append("\n");
        }

        sb.append(functions.toString());

        sb.append("int main() {\n");
        indentLevel++;

        sb.append(indent()).append("if (init_framebuffer() != 0) {\n");
        sb.append(indent()).append("    printf(\"Error: No se pudo inicializar el framebuffer\\n\");\n");
        sb.append(indent()).append("    return 1;\n");
        sb.append(indent()).append("}\n\n");

        sb.append(indent()).append("clear_screen();\n\n");

        if (mainInitializations.length() > 0) {
            sb.append(indent()).append("// Variable initializations\n");
            sb.append(mainInitializations.toString());
            sb.append("\n");
        }

        sb.append(mainCode.toString());

        sb.append("\n");
        sb.append(indent()).append("wait_seconds(3);\n");
        sb.append(indent()).append("cleanup_framebuffer();\n");
        sb.append(indent()).append("return 0;\n");

        indentLevel--;
        sb.append("}\n");

        return sb.toString();
    }

    // MODIFICADO: Método visitType con soporte para double
    @Override
    public String visitType(VGraphParser.TypeContext ctx) {
        if (ctx.INT() != null) {
            return "int";
        } else if (ctx.COLOR() != null) {
            return "char*";
        } else if (ctx.DOUBLE() != null) {
            return "double";  // NUEVO
        }
        return "int";
    }

    // MODIFICADO: Método visitVar_decl con soporte para double
    @Override
    public String visitVar_decl(VGraphParser.Var_declContext ctx) {
        StringBuilder sb = new StringBuilder();
        String type = visit(ctx.type());

        if (ctx.expression() != null) {
            String varName = resolveVariableName(ctx.id1.getText());
            String value = visit(ctx.expression());

            // MODIFICADO: Solo convertir a entero si el tipo es int
            if (type.equals("int") && value.contains(".")) {
                try {
                    double doubleValue = Double.parseDouble(value);
                    value = String.valueOf((int) doubleValue);
                } catch (NumberFormatException e) {
                    // Si no es un número directo, mantener la expresión
                }
            }
            // Si es double, mantener el valor decimal tal como está

            sb.append(type).append(" ").append(varName).append(" = ").append(value).append(";\n");
        } else {
            sb.append(type).append(" ");
            String firstVar = resolveVariableName(ctx.id1.getText());
            sb.append(firstVar);

            List<TerminalNode> allIds = ctx.ID();
            for (int i = 1; i < allIds.size(); i++) {
                String varName = resolveVariableName(allIds.get(i).getText());
                sb.append(", ").append(varName);
            }
            sb.append(";\n");
        }

        return sb.toString();
    }

    // MODIFICADO: Método visitVar_assign (sin cambios en lógica, pero documentado)
    @Override
    public String visitVar_assign(VGraphParser.Var_assignContext ctx) {
        String expr = visit(ctx.expression());
        String varName = resolveVariableName(ctx.ID().getText());

        // NOTA: Aquí podrías usar variableTypes.get(varName) para obtener el tipo
        // y decidir si convertir o no, pero por simplicidad mantenemos como estaba

        if (expr.contains(".")) {
            try {
                double doubleValue = Double.parseDouble(expr);
                // Solo convertir si sabemos que es int (opcional)
                String varType = variableTypes.get(varName);
                if ("int".equals(varType)) {
                    expr = String.valueOf((int) doubleValue);
                }
            } catch (NumberFormatException e) {
                // Si no es un número directo, mantener la expresión original
            }
        }

        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return indent() + varName + " = " + expr + ";\n";
        } else {
            return indent() + varName + " = " + expr + ";\n";
        }
    }

    // Los demás métodos se mantienen iguales...
    // (visitSentence, visitFrame, visitPrintln, etc. - sin cambios)

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
        } else if (ctx.clear_command() != null) {
            return visit(ctx.clear_command());
        }
        return "";
    }

    @Override
    public String visitFrame(VGraphParser.FrameContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("// === FRAME START ===\n");

        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            sb.append(visit(sentence));
        }

        sb.append(indent()).append("// === FRAME END ===\n");
        return sb.toString();
    }

    @Override
    public String visitPrintln(VGraphParser.PrintlnContext ctx) {
        String expr = visit(ctx.expression());
        return indent() + "println_int(" + expr + ");\n";
    }

    @Override
    public String visitLoop_command(VGraphParser.Loop_commandContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append(visit(ctx.e1));
        sb.append(indent()).append("while (").append(visit(ctx.e2)).append(") {\n");
        indentLevel++;
        sb.append(visit(ctx.e4));
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
        String varName = resolveVariableName(ctx.ID().getText());
        return indent() + varName + " = " + expr + ";\n";
    }

    @Override
    public String visitComparison(VGraphParser.ComparisonContext ctx) {
        String left = visit(ctx.e1);
        String right = visit(ctx.e2);

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

    @Override
    public String visitConditional(VGraphParser.ConditionalContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent()).append("if (").append(visit(ctx.expression(0))).append(") {\n");
        indentLevel++;

        List<VGraphParser.SentenceContext> allSentences = ctx.sentence();
        List<VGraphParser.ExpressionContext> allExpressions = ctx.expression();

        int elseifCount = (ctx.ELSEIF() != null) ? ctx.ELSEIF().size() : 0;
        boolean hasElse = (ctx.ELSE() != null);

        int totalSections = 1 + elseifCount + (hasElse ? 1 : 0);
        int sentencesPerSection = allSentences.size() / totalSections;
        int currentIndex = 0;

        int ifEnd = Math.min(sentencesPerSection, allSentences.size());
        for (int i = currentIndex; i < ifEnd; i++) {
            sb.append(visit(allSentences.get(i)));
        }
        currentIndex = ifEnd;

        indentLevel--;
        sb.append(indent()).append("}");

        for (int elseifIndex = 0; elseifIndex < elseifCount; elseifIndex++) {
            if (elseifIndex + 1 < allExpressions.size()) {
                sb.append(" else if (").append(visit(allExpressions.get(elseifIndex + 1))).append(") {\n");
                indentLevel++;

                int elseifEnd = Math.min(currentIndex + sentencesPerSection, allSentences.size());
                for (int i = currentIndex; i < elseifEnd; i++) {
                    sb.append(visit(allSentences.get(i)));
                }
                currentIndex = elseifEnd;

                indentLevel--;
                sb.append(indent()).append("}");
            }
        }

        if (hasElse) {
            sb.append(" else {\n");
            indentLevel++;

            for (int i = currentIndex; i < allSentences.size(); i++) {
                sb.append(visit(allSentences.get(i)));
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
            return resolveVariableName(ctx.ID().getText());
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
        return visit(ctx.s);
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
            String arg1Name = resolveVariableName(ctx.arg1.getText());
            sb.append("int ").append(arg1Name);

            List<TerminalNode> allIds = ctx.ID();
            for (int i = 2; i < allIds.size(); i++) {
                String argName = resolveVariableName(allIds.get(i).getText());
                sb.append(", int ").append(argName);
            }
        }

        sb.append(") {\n");
        indentLevel++;

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

            List<VGraphParser.ExpressionContext> allExpressions = ctx.expression();
            for (int i = 1; i < allExpressions.size(); i++) {
                sb.append(", ").append(visit(allExpressions.get(i)));
            }
        }

        sb.append(");\n");
        return sb.toString();
    }
}