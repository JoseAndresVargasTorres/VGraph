package v.graph;

import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.*;

/**
 * Validador semántico completo con soporte para double y validación de tipos
 */
public class SemanticValidator extends VGraphBaseVisitor<String> {

    private Map<String, String> symbolTable = new HashMap<>();
    private Map<String, FunctionInfo> functionTable = new HashMap<>();
    private List<String> semanticErrors = new ArrayList<>();

    // Tipos válidos del lenguaje
    private static final Set<String> VALID_TYPES = new HashSet<>(Arrays.asList("int", "color", "double"));

    // Colores válidos según la gramática
    private static final Set<String> VALID_COLORS = new HashSet<>(Arrays.asList(
            "negro", "blanco", "rojo", "verde", "azul", "amarillo", "cyan", "magenta", "marron"
    ));

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
    public String visitProgram(VGraphParser.ProgramContext ctx) {
        clearErrors();

        // Primera pasada: recolectar declaraciones de funciones
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            if (sentence.function() != null) {
                collectFunction(sentence.function());
            }
        }

        // Segunda pasada: validar declaraciones de variables
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            if (sentence.var_decl() != null) {
                visit(sentence.var_decl());
            }
        }

        // Tercera pasada: validar uso y resto del código
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            if (sentence.var_decl() == null && sentence.function() == null) {
                visit(sentence);
            }
        }

        return null;
    }

    @Override
    public String visitVar_decl(VGraphParser.Var_declContext ctx) {
        String type = ctx.type().getText();
        int line = ctx.getStart().getLine();

        // Validar que el tipo sea válido
        if (!VALID_TYPES.contains(type)) {
            addError("Unknown type '" + type + "'", line);
            return type; // Continuar para encontrar más errores
        }

        // Caso 1: Declaración con asignación (int) x = expr;
        if (ctx.expression() != null) {
            String varName = ctx.id1.getText();
            if (symbolTable.containsKey(varName)) {
                addError("Variable '" + varName + "' is already declared", line);
            } else {
                symbolTable.put(varName, type);
            }

            // Validar la expresión y verificar compatibilidad de tipos
            String exprType = visit(ctx.expression());
            if (exprType != null && !isTypeCompatible(type, exprType)) {
                addError("Cannot assign " + exprType + " to variable '" + varName +
                        "' of type " + type, line);
            }
        }
        // Caso 2: Declaración múltiple (int) x, y, z;
        else {
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
        }

        return type;
    }

    @Override
    public String visitVar_assign(VGraphParser.Var_assignContext ctx) {
        String varName = ctx.ID().getText();
        int line = ctx.getStart().getLine();

        if (!symbolTable.containsKey(varName)) {
            addError("Variable '" + varName + "' is not declared", line);
            return null;
        }

        String varType = symbolTable.get(varName);
        String exprType = visit(ctx.expression());

        if (exprType != null && !isTypeCompatible(varType, exprType)) {
            addError("Cannot assign " + exprType + " to variable '" + varName +
                    "' of type " + varType, line);
        }

        return varType;
    }

    @Override
    public String visitExpression(VGraphParser.ExpressionContext ctx) {
        if (ctx.operand() != null) {
            return visit(ctx.operand());
        } else if (ctx.comparison() != null) {
            visit(ctx.comparison());
            return "boolean";
        }
        return null;
    }

    @Override
    public String visitOperand(VGraphParser.OperandContext ctx) {
        String leftType = visit(ctx.t1);

        // Si hay operaciones matemáticas, verificar tipos
        if (ctx.factor().size() > 1) {
            for (int i = 1; i < ctx.factor().size(); i++) {
                String rightType = visit(ctx.factor(i));
                if (!isNumericType(leftType) || !isNumericType(rightType)) {
                    addError("Arithmetic operations require numeric types", ctx.getStart().getLine());
                    return "error";
                }
            }
            // Las operaciones aritméticas entre int y double resultan en double
            return containsDouble(ctx) ? "double" : "int";
        }

        return leftType;
    }

    @Override
    public String visitFactor(VGraphParser.FactorContext ctx) {
        String leftType = visit(ctx.t1);

        // Si hay operaciones matemáticas, verificar tipos
        if (ctx.term().size() > 1) {
            for (int i = 1; i < ctx.term().size(); i++) {
                String rightType = visit(ctx.term(i));
                if (!isNumericType(leftType) || !isNumericType(rightType)) {
                    addError("Arithmetic operations require numeric types", ctx.getStart().getLine());
                    return "error";
                }
            }
            return containsDoubleInTerms(ctx) ? "double" : "int";
        }

        return leftType;
    }

    @Override
    public String visitTerm(VGraphParser.TermContext ctx) {
        if (ctx.NUMBER() != null) {
            String number = ctx.NUMBER().getText();
            return number.contains(".") ? "double" : "int";
        } else if (ctx.COLOR_VALUES() != null) {
            String colorValue = ctx.COLOR_VALUES().getText();
            if (!VALID_COLORS.contains(colorValue)) {
                addError("Invalid color '" + colorValue + "'", ctx.getStart().getLine());
            }
            return "color";
        } else if (ctx.BOOLEAN() != null) {
            return "boolean";
        } else if (ctx.ID() != null) {
            String varName = ctx.ID().getText();
            if (!symbolTable.containsKey(varName)) {
                addError("Variable '" + varName + "' is not declared", ctx.getStart().getLine());
                return "error";
            }
            return symbolTable.get(varName);
        } else if (ctx.expression() != null) {
            return visit(ctx.expression());
        } else if (ctx.cos() != null) {
            visit(ctx.cos());
            return "double";
        } else if (ctx.sin() != null) {
            visit(ctx.sin());
            return "double";
        }
        return null;
    }

    @Override
    public String visitSin(VGraphParser.SinContext ctx) {
        String exprType = visit(ctx.expression());
        if (exprType != null && !isNumericType(exprType)) {
            addError("sin() function requires numeric argument", ctx.getStart().getLine());
        }
        return "double";
    }

    @Override
    public String visitCos(VGraphParser.CosContext ctx) {
        String exprType = visit(ctx.expression());
        if (exprType != null && !isNumericType(exprType)) {
            addError("cos() function requires numeric argument", ctx.getStart().getLine());
        }
        return "double";
    }

    @Override
    public String visitComparison(VGraphParser.ComparisonContext ctx) {
        String leftType = visit(ctx.e1);
        String rightType = visit(ctx.e2);

        if (leftType != null && rightType != null) {
            // Solo permitir comparaciones entre tipos compatibles
            if (!areComparableTypes(leftType, rightType)) {
                addError("Cannot compare " + leftType + " with " + rightType,
                        ctx.getStart().getLine());
            }
        }
        return "boolean";
    }

    @Override
    public String visitSetcolor(VGraphParser.SetcolorContext ctx) {
        String exprType = visit(ctx.t);
        if (exprType != null && !exprType.equals("color")) {
            addError("setcolor() requires a color argument", ctx.getStart().getLine());
        }
        return null;
    }

    @Override
    public String visitShapeCall(VGraphParser.ShapeCallContext ctx) {
        if (ctx.LINE() != null) {
            validateCoordinate(ctx.a, "x1");
            validateCoordinate(ctx.b, "y1");
            validateCoordinate(ctx.c, "x2");
            validateCoordinate(ctx.d, "y2");
        } else if (ctx.RECT() != null) {
            validateCoordinate(ctx.x, "x");
            validateCoordinate(ctx.y, "y");
            validateNumeric(ctx.w, "width");
            validateNumeric(ctx.h, "height");
        } else if (ctx.CIRCLE() != null) {
            validateCoordinate(ctx.x, "x");
            validateCoordinate(ctx.y, "y");
            validateNumeric(ctx.r, "radius");
        } else if (ctx.PIXEL() != null) {
            validateCoordinate(ctx.x, "x");
            validateCoordinate(ctx.y, "y");
        }
        return null;
    }

    @Override
    public String visitFunCall(VGraphParser.FunCallContext ctx) {
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

        // Validar argumentos
        for (VGraphParser.ExpressionContext expr : ctx.expression()) {
            visit(expr);
        }

        return null;
    }

    @Override
    public String visitFunction(VGraphParser.FunctionContext ctx) {
        // Crear nuevo scope local
        Map<String, String> previousScope = new HashMap<>(symbolTable);

        // Agregar parámetros al scope local
        if (ctx.arg1 != null) {
            symbolTable.put(ctx.arg1.getText(), "int"); // Los parámetros son int por defecto

            List<TerminalNode> allIds = ctx.ID();
            for (int i = 2; i < allIds.size(); i++) {
                symbolTable.put(allIds.get(i).getText(), "int");
            }
        }

        // Validar cuerpo de la función
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            visit(sentence);
        }

        // Restaurar scope anterior
        symbolTable = previousScope;
        return null;
    }

    @Override
    public String visitLoop_command(VGraphParser.Loop_commandContext ctx) {
        visit(ctx.e1); // inicialización
        String condType = visit(ctx.e2); // condición
        visit(ctx.e3); // incremento
        visit(ctx.e4); // cuerpo

        if (condType != null && !condType.equals("boolean")) {
            addError("Loop condition must be boolean", ctx.getStart().getLine());
        }

        return null;
    }

    @Override
    public String visitConditional(VGraphParser.ConditionalContext ctx) {
        // Validar condición principal
        for (VGraphParser.ExpressionContext expr : ctx.expression()) {
            String condType = visit(expr);
            if (condType != null && !condType.equals("boolean")) {
                addError("Condition must be boolean", expr.getStart().getLine());
            }
        }

        // Validar cuerpos
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            visit(sentence);
        }

        return null;
    }

    @Override
    public String visitIncrement_loop(VGraphParser.Increment_loopContext ctx) {
        String varName = ctx.ID().getText();
        int line = ctx.getStart().getLine();

        if (!symbolTable.containsKey(varName)) {
            addError("Variable '" + varName + "' is not declared", line);
            return null;
        }

        String varType = symbolTable.get(varName);
        String exprType = visit(ctx.expression());

        if (exprType != null && !isTypeCompatible(varType, exprType)) {
            addError("Cannot assign " + exprType + " to variable '" + varName +
                    "' of type " + varType, line);
        }

        return null;
    }

    @Override
    public String visitPrintln(VGraphParser.PrintlnContext ctx) {
        visit(ctx.expression());
        return null;
    }

    @Override
    public String visitWait_command(VGraphParser.Wait_commandContext ctx) {
        String exprType = visit(ctx.e);
        if (exprType != null && !isNumericType(exprType)) {
            addError("wait() requires numeric argument", ctx.getStart().getLine());
        }
        return null;
    }

    @Override
    public String visitClear_command(VGraphParser.Clear_commandContext ctx) {
        // clear() no tiene parámetros, validación simple
        return null;
    }

    @Override
    public String visitFrame(VGraphParser.FrameContext ctx) {
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            visit(sentence);
        }
        return null;
    }

    @Override
    public String visitBody(VGraphParser.BodyContext ctx) {
        for (VGraphParser.SentenceContext sentence : ctx.sentence()) {
            visit(sentence);
        }
        return null;
    }

    @Override
    public String visitDraw(VGraphParser.DrawContext ctx) {
        visit(ctx.s);
        return null;
    }

    // Métodos auxiliares
    private void validateCoordinate(VGraphParser.ExpressionContext expr, String coordName) {
        if (expr != null) {
            String type = visit(expr);
            if (type != null && !isNumericType(type)) {
                addError("Coordinate " + coordName + " must be numeric", expr.getStart().getLine());
            }
        }
    }

    private void validateNumeric(VGraphParser.ExpressionContext expr, String paramName) {
        if (expr != null) {
            String type = visit(expr);
            if (type != null && !isNumericType(type)) {
                addError("Parameter " + paramName + " must be numeric", expr.getStart().getLine());
            }
        }
    }

    private boolean isTypeCompatible(String targetType, String sourceType) {
        if (targetType.equals(sourceType)) {
            return true;
        }

        // int y double son compatibles entre sí
        if ((targetType.equals("int") || targetType.equals("double")) &&
                (sourceType.equals("int") || sourceType.equals("double"))) {
            return true;
        }

        return false;
    }

    private boolean isNumericType(String type) {
        return "int".equals(type) || "double".equals(type);
    }

    private boolean areComparableTypes(String type1, String type2) {
        // Números se pueden comparar entre sí
        if (isNumericType(type1) && isNumericType(type2)) {
            return true;
        }
        // Tipos iguales se pueden comparar
        if (type1.equals(type2)) {
            return true;
        }
        return false;
    }

    private boolean containsDouble(VGraphParser.OperandContext ctx) {
        for (VGraphParser.FactorContext factor : ctx.factor()) {
            if (containsDoubleInTerms(factor)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDoubleInTerms(VGraphParser.FactorContext ctx) {
        for (VGraphParser.TermContext term : ctx.term()) {
            if (visit(term) != null && visit(term).equals("double")) {
                return true;
            }
        }
        return false;
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