package v.ast;

import java.util.Map;

public class LessThan implements ASTNode{
    private ASTNode expression1;
    private ASTNode expression2;

    public LessThan(ASTNode expression1, ASTNode expression2){
        super();
        this.expression1 = expression1;
        this.expression2 = expression2;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        Object left = expression1.execute(symbolTable);
        Object right = expression2.execute(symbolTable);

        int leftValue = (left instanceof Double) ? ((Double) left).intValue() : (int) left;
        int rightValue = (right instanceof Double) ? ((Double) right).intValue() : (int) right;

        return leftValue < rightValue;
    }

}