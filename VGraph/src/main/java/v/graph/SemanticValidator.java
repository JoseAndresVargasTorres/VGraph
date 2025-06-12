package v.graph;

import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.*;

/**
 * Validador semántico con soporte para double
 */
public class SemanticValidator extends VGraphBaseVisitor<Void> {

    private Map<String, String> symbolTable = new HashMap<>();
    private Map<String, FunctionInfo> functionTable = new HashMap<>();
    private List<String> semanticErrors = new ArrayList<>();

    // NUEVO: Set de tipos válidos
    private static final Set<String> VALID_TYPES = new HashSet<>(Arrays.asList("int", "color", "double"));

    private static class FunctionInfo {
        String name;
        List<String> parameters;

        FunctionInfo(String name, List<String> parameters) {
            this.name = name;
            this.parameters = parameters;
        }
    }

    public List<String> getSemanticErrors() {
        return semanticErrors;
    }

    public boolean hasErrors() {
        return !semanticErrors.isEmpty();
    }

    public void clearErrors() {
        semanticErrors.clear();
        symbolTable.clear();
        functionTable.clear();
    }

    @Override
    public Void visitProgram(VGraphParser.ProgramContext ctx) {
        clearErrors();

        // Primera pasada: recolectar declaraciones
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            if (sentence.var_decl() != null) {
                visit(sentence.var_decl());
            } else if (sentence.function() != null) {
                collectFunction(sentence.function());
            }
        }

        // Segunda pasada: validar uso
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            if (sentence.var_decl() == null) {
                visit(sentence);
            }
        }

        return null;
    }

    @Override
    public Void visitVar_decl(VGraphParser.Var_declContext ctx) {
        String type = ctx.type().getText();
        int line = ctx.getStart().getLine();

        // NUEVO: Validar que el tipo sea válido
        if (!VALID_TYPES.contains(type)) {
            addError("Unknown type '" + type + "'", line);
            return null; // No continuar si el tipo es inválido
        }

        // Variable principal (id1)
        String varName = ctx.id1.getText();
        if (symbolTable.containsKey(varName)) {
            addError("Variable '" + varName + "' is already declared", line);
        } else {
            symbolTable.put(varName, type);
        }

        // Variables adicionales
        List<TerminalNode> additionalIds = ctx.ID();
        for (int i = 1; i < additionalIds.size(); i++) {
            String additionalVar = additionalIds.get(i).getText();
            if (symbolTable.containsKey(additionalVar)) {
                addError("Variable '" + additionalVar + "' is already declared", line);
            } else {
                symbolTable.put(additionalVar, type);
            }
        }

        return null;
    }

    @Override
    public Void visitVar_assign(VGraphParser.Var_assignContext ctx) {
        String varName = ctx.ID().getText();
        int line = ctx.getStart().getLine();

        if (!symbolTable.containsKey(varName)) {
            addError("Variable '" + varName + "' is not declared", line);
        }

        visit(ctx.expression());
        return null;
    }

    @Override
    public Void visitTerm(VGraphParser.TermContext ctx) {
        if (ctx.ID() != null) {
            String varName = ctx.ID().getText();
            if (!symbolTable.containsKey(varName)) {
                int line = ctx.getStart().getLine();
                addError("Variable '" + varName + "' is not declared", line);
            }
        }

        return super.visitTerm(ctx);
    }

    @Override
    public Void visitSetcolor(VGraphParser.SetcolorContext ctx) {
        visit(ctx.t);
        return null;
    }

    @Override
    public Void visitFunCall(VGraphParser.FunCallContext ctx) {
        String funcName = ctx.funID.getText();
        int line = ctx.getStart().getLine();

        if (!functionTable.containsKey(funcName)) {
            addError("Function '" + funcName + "' is not declared", line);
        } else {
            FunctionInfo funcInfo = functionTable.get(funcName);
            int expectedParams = funcInfo.parameters.size();
            int actualParams = ctx.expression().size();

            if (expectedParams != actualParams) {
                addError("Function '" + funcName + "' expects " + expectedParams +
                        " parameters, but " + actualParams + " were provided", line);
            }
        }

        for (VGraphParser.ExpressionContext expr : ctx.expression()) {
            visit(expr);
        }

        return null;
    }

    @Override
    public Void visitIncrement_loop(VGraphParser.Increment_loopContext ctx) {
        String varName = ctx.ID().getText();
        int line = ctx.getStart().getLine();

        if (!symbolTable.containsKey(varName)) {
            addError("Variable '" + varName + "' is not declared", line);
        }

        visit(ctx.expression());
        return null;
    }

    @Override
    public Void visitLoop_command(VGraphParser.Loop_commandContext ctx) {
        visit(ctx.e1);
        visit(ctx.e2);
        visit(ctx.e3);
        visit(ctx.e4);
        return null;
    }

    @Override
    public Void visitComparison(VGraphParser.ComparisonContext ctx) {
        visit(ctx.e1);
        visit(ctx.e2);
        return null;
    }

    @Override
    public Void visitConditional(VGraphParser.ConditionalContext ctx) {
        for (VGraphParser.ExpressionContext expr : ctx.expression()) {
            visit(expr);
        }

        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            visit(sentence);
        }

        return null;
    }

    @Override
    public Void visitFrame(VGraphParser.FrameContext ctx) {
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            visit(sentence);
        }
        return null;
    }

    @Override
    public Void visitDraw(VGraphParser.DrawContext ctx) {
        visit(ctx.s);
        return null;
    }

    @Override
    public Void visitShapeCall(VGraphParser.ShapeCallContext ctx) {
        if (ctx.LINE() != null) {
            visit(ctx.a);
            visit(ctx.b);
            visit(ctx.c);
            visit(ctx.d);
        } else if (ctx.RECT() != null) {
            visit(ctx.x);
            visit(ctx.y);
            visit(ctx.w);
            visit(ctx.h);
        } else if (ctx.CIRCLE() != null) {
            visit(ctx.x);
            visit(ctx.y);
            visit(ctx.r);
        } else if (ctx.PIXEL() != null) {
            visit(ctx.x);
            visit(ctx.y);
        }

        return null;
    }

    @Override
    public Void visitFunction(VGraphParser.FunctionContext ctx) {
        Map<String, String> previousScope = new HashMap<>(symbolTable);

        if (ctx.arg1 != null) {
            symbolTable.put(ctx.arg1.getText(), "int");

            List<TerminalNode> allIds = ctx.ID();
            for (int i = 2; i < allIds.size(); i++) {
                symbolTable.put(allIds.get(i).getText(), "int");
            }
        }

        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            visit(sentence);
        }

        symbolTable = previousScope;
        return null;
    }

    @Override
    public Void visitPrintln(VGraphParser.PrintlnContext ctx) {
        visit(ctx.expression());
        return null;
    }

    @Override
    public Void visitWait_command(VGraphParser.Wait_commandContext ctx) {
        visit(ctx.e);
        return null;
    }

    @Override
    public Void visitBody(VGraphParser.BodyContext ctx) {
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            visit(sentence);
        }
        return null;
    }

    private void collectFunction(VGraphParser.FunctionContext ctx) {
        String funcName = ctx.funID.getText();
        int line = ctx.getStart().getLine();

        if (functionTable.containsKey(funcName)) {
            addError("Function '" + funcName + "' is already declared", line);
        } else {
            List<String> parameters = new ArrayList<>();

            if (ctx.arg1 != null) {
                parameters.add(ctx.arg1.getText());

                List<TerminalNode> allIds = ctx.ID();
                for (int i = 2; i < allIds.size(); i++) {
                    parameters.add(allIds.get(i).getText());
                }
            }

            functionTable.put(funcName, new FunctionInfo(funcName, parameters));
        }
    }

    private void addError(String message, int line) {
        semanticErrors.add("Error at line " + line + " - " + message);
    }
}