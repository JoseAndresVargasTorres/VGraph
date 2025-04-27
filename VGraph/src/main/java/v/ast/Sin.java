package v.ast;

import java.util.Map;

public class Sin implements ASTNode{
    public ASTNode expression;

    public Sin(ASTNode expression) {
        this.expression = expression;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        return Math.sin((double)expression.execute(symbolTable));
    }
}
