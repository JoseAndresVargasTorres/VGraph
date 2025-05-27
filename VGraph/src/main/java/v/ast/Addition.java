package v.ast;

import java.util.Map;

public class Addition implements ASTNode{
    private ASTNode operand1;
    private ASTNode operand2;

    public Addition(ASTNode operand1, ASTNode operand2) {
        super();
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable){
        Object left = operand1.execute(symbolTable);
        Object right = operand2.execute(symbolTable);

        // Convertir left a entero
        int leftValue;
        if (left instanceof Integer) {
            leftValue = (Integer) left;
        } else if (left instanceof Double) {
            leftValue = ((Double) left).intValue();
        } else if (left instanceof String) {
            leftValue = Integer.parseInt((String) left);
        } else {
            leftValue = 0; // valor por defecto
        }

        // Convertir right a entero
        int rightValue;
        if (right instanceof Integer) {
            rightValue = (Integer) right;
        } else if (right instanceof Double) {
            rightValue = ((Double) right).intValue();
        } else if (right instanceof String) {
            rightValue = Integer.parseInt((String) right);
        } else {
            rightValue = 0; // valor por defecto
        }

        return leftValue + rightValue;
    }
}
