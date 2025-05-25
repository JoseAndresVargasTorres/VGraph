package v.graph;

import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.List;

public class VGraphCustomVisitor extends VGraphBaseVisitor<String> {
    private int indentLevel = 0;
    private final StringBuilder includes = new StringBuilder();

    public VGraphCustomVisitor() {
        includes.append("#include <stdio.h>\n");
        includes.append("#include <math.h>\n");
        includes.append("#include <unistd.h>\n\n");
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

        // Generar código para cada sentencia
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            sb.append(visit(sentence));
        }

        return sb.toString();
    }

    @Override
    public String visitPrintln(VGraphParser.PrintlnContext ctx) {
        String expr = visit(ctx.expression());
        return indent() + "printf(\"%d\\n\", " + expr + ");\n";
    }

    @Override
    public String visitVar_decl(VGraphParser.Var_declContext ctx) {
        StringBuilder sb = new StringBuilder();
        String type = visit(ctx.type());

        // Primera variable
        sb.append(indent()).append(type).append(" ").append(ctx.ID(0).getText());

        // Variables adicionales
        for (int i = 1; i < ctx.ID().size(); i++) {
            sb.append(", ").append(ctx.ID(i).getText());
        }

        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public String visitType(VGraphParser.TypeContext ctx) {
        if (ctx.INT() != null) {
            return "int";
        } else if (ctx.COLOR() != null) {
            return "int"; // Representamos colores como enteros
        }
        return "int"; // Tipo por defecto
    }

    @Override
    public String visitVar_assign(VGraphParser.Var_assignContext ctx) {
        String expr = visit(ctx.expression());
        return indent() + ctx.ID().getText() + " = " + expr + ";\n";
    }

    @Override
    public String visitLoop_command(VGraphParser.Loop_commandContext ctx) {
        StringBuilder sb = new StringBuilder();

        // Inicialización del loop
        sb.append(visit(ctx.var_assign()));

        sb.append(indent()).append("while (").append(visit(ctx.comparison())).append(") {\n");
        indentLevel++;

        // Cuerpo del loop
        sb.append(visit(ctx.body()));

        // Incremento
        sb.append(visit(ctx.increment_loop()));

        indentLevel--;
        sb.append(indent()).append("}\n");

        return sb.toString();
    }

    @Override
    public String visitComparison(VGraphParser.ComparisonContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));

        // Determinar qué operador de comparación se está usando
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

        // Parte if
        sb.append(indent()).append("if (").append(visit(ctx.expression())).append(") {\n");
        indentLevel++;
        for (VGraphParser.SentenceContext stmt : ctx.sentence()) {
            sb.append(visit(stmt));
        }
        indentLevel--;
        sb.append(indent()).append("}\n");

        // Parte elseif
        if (ctx.ELSEIF() != null) {
            sb.append(indent()).append("else if (1) {\n"); // Cambiar por condición real si es necesario
            indentLevel++;
            for (VGraphParser.SentenceContext stmt : ctx.sentence()) {
                sb.append(visit(stmt));
            }
            indentLevel--;
            sb.append(indent()).append("}\n");
        }

        // Parte else
        if (ctx.ELSE() != null) {
            sb.append(indent()).append("else {\n");
            indentLevel++;
            for (VGraphParser.SentenceContext stmt : ctx.sentence()) {
                sb.append(visit(stmt));
            }
            indentLevel--;
            sb.append(indent()).append("}\n");
        }

        return sb.toString();
    }

    @Override
    public String visitExpression(VGraphParser.ExpressionContext ctx) {
        return visit(ctx.operand());
    }

    @Override
    public String visitOperand(VGraphParser.OperandContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.factor(0)));

        // Manejar operadores si existen
        for (int i = 1; i < ctx.factor().size(); i++) {
            // Determinar el operador basado en los tokens
            TerminalNode opNode = (TerminalNode) ctx.getChild(2 * i - 1);
            String op = opNode.getText();
            sb.append(" ").append(op).append(" ").append(visit(ctx.factor(i)));
        }

        return sb.toString();
    }

    @Override
    public String visitFactor(VGraphParser.FactorContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.term(0)));

        // Manejar operadores si existen
        for (int i = 1; i < ctx.term().size(); i++) {
            // Determinar el operador basado en los tokens
            TerminalNode opNode = (TerminalNode) ctx.getChild(2 * i - 1);
            String op = opNode.getText();
            sb.append(" ").append(op).append(" ").append(visit(ctx.term(i)));
        }

        return sb.toString();
    }

    @Override
    public String visitTerm(VGraphParser.TermContext ctx) {
        if (ctx.NUMBER() != null) {
            return ctx.NUMBER().getText();
        } else if (ctx.COLOR_VALUES() != null) {
            // Mapear colores a valores numéricos
            return mapColorToValue(ctx.COLOR_VALUES().getText());
        } else if (ctx.BOOLEAN() != null) {
            return ctx.BOOLEAN().getText();
        } else if (ctx.ID() != null) {
            return ctx.ID().getText();
        } else if (ctx.expression() != null) {
            return "(" + visit(ctx.expression()) + ")";
        } else if (ctx.cos() != null) {
            return visit(ctx.cos());
        } else if (ctx.sin() != null) {
            return visit(ctx.sin());
        }
        return "0"; // Valor por defecto
    }

    private String mapColorToValue(String color) {
        switch (color) {
            case "negro": return "0";
            case "blanco": return "1";
            case "rojo": return "2";
            case "verde": return "3";
            case "azul": return "4";
            case "amarillo": return "5";
            case "cyan": return "6";
            case "magenta": return "7";
            case "marron": return "8";
            default: return "0";
        }
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
        String time = visit(ctx.expression());
        return indent() + "sleep(" + time +");\n";
    }

    @Override
    public String visitSetcolor(VGraphParser.SetcolorContext ctx) {
        return indent() + "// setcolor(" + visit(ctx.expression()) + ")\n";
    }

    @Override
    public String visitDraw(VGraphParser.DrawContext ctx) {
        return visit(ctx.shapeCall());
    }

    @Override
    public String visitShapeCall(VGraphParser.ShapeCallContext ctx) {
        if (ctx.LINE() != null) {
            return indent() + "// draw line from (" + visit(ctx.expression(0)) + "," + visit(ctx.expression(1)) +
                    ") to (" + visit(ctx.expression(2)) + "," + visit(ctx.expression(3)) + ")\n";
        } else if (ctx.RECT() != null) {
            return indent() + "// draw rectangle at (" + visit(ctx.expression(0)) + "," + visit(ctx.expression(1)) +
                    ") with size " + visit(ctx.expression(2)) + "x" + visit(ctx.expression(3)) + "\n";
        } else if (ctx.CIRCLE() != null) {
            return indent() + "// draw circle at (" + visit(ctx.expression(0)) + "," + visit(ctx.expression(1)) +
                    ") with radius " + visit(ctx.expression(2)) + "\n";
        } else if (ctx.PIXEL() != null) {
            return indent() + "// draw pixel at (" + visit(ctx.expression(0)) + "," + visit(ctx.expression(1)) + ")\n";
        }
        return "";
    }

    @Override
    public String visitFunction(VGraphParser.FunctionContext ctx) {
        StringBuilder sb = new StringBuilder();

        // Declaración de función
        sb.append("void ").append(ctx.funID.getText()).append("(");

        // Parámetros
        if (ctx.arg1 != null) {
            sb.append("int ").append(ctx.arg1.getText());
            // Si arg2 es un solo token (no lista)
            if (ctx.arg2 != null) {
                sb.append(", int ").append(ctx.arg2.getText());
            }
        }

        sb.append(") {\n");
        indentLevel++;

        // Cuerpo de la función
        for (VGraphParser.SentenceContext stmt : ctx.sentence()) {
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

        // Argumentos
        if (ctx.arg1 != null) {
            sb.append(visit(ctx.arg1));
            // Si arg2 es un solo token (no lista)
            if (ctx.arg2 != null) {
                sb.append(", ").append(visit(ctx.arg2));
            }
        }

        sb.append(");\n");
        return sb.toString();
    }

    @Override
    public String visitClear_command(VGraphParser.Clear_commandContext ctx) {
        return indent() + "// clear screen\n";
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
}