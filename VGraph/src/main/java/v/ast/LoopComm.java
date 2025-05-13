package v.ast;

import java.util.List;
import java.util.Map;

public class LoopComm implements ASTNode{
    private ASTNode init; //VarDecl
    private ASTNode condition; // Comparación
    private ASTNode update; //VarAssign
    private List<ASTNode> body;

    public LoopComm(ASTNode init, ASTNode condition, ASTNode update, List<ASTNode> body) {
        super();
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    @Override
    public Object execute(java.util.Map<String, Object> symbolTable) {
        init.execute(symbolTable);

        while ((boolean)condition.execute(symbolTable)){
            for (ASTNode expr: body){
                expr.execute(symbolTable);
            }
            update.execute(symbolTable);
        }
        return null;
    }

    private boolean evaluateCondition(Map<String, Object> symbolTable){
        Object result = condition.execute(symbolTable);
        if (result instanceof Boolean){
            return (boolean)result;
        } else{
            throw new RuntimeException("error in the loop condition");
        }
    }
}
