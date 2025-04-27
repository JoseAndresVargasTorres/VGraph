package v.ast;

import java.util.Map;

public class Cos implements ASTNode{
    public ASTNode expression;

    public Cos(ASTNode expression) {
        this.expression = expression;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        return Math.cos((double)expression.execute(symbolTable));
    }
}
