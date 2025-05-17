package v.ast;

import java.util.Map;

public class GreaterOrEqual implements ASTNode{
    private ASTNode expression1;
    private ASTNode expression2;

    public GreaterOrEqual(ASTNode expression1, ASTNode expression2){
        super();
        this.expression1 = expression1;
        this.expression2 = expression2;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable){
        return ((int)expression1.execute(symbolTable) <= (int)expression2.execute(symbolTable));
    }
}
