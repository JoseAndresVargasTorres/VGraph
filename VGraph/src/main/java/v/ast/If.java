package v.ast;


import java.util.List;
import java.util.Map;

public class If implements ASTNode {
    private ASTNode condition;
    private List<ASTNode> ifBody;
    private List<ConditionalBlock> elseifBlocks;
    private List<ASTNode> elseBody; // puede ser null

    public If(ASTNode condition, List<ASTNode> ifBody, List<ConditionalBlock> elseifBlocks, List<ASTNode> elseBody) {
        this.condition = condition;
        this.ifBody = ifBody;
        this.elseifBlocks = elseifBlocks;
        this.elseBody = elseBody;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        if ((boolean) condition.execute(symbolTable)) {
            for (ASTNode n : ifBody) n.execute(symbolTable);
        } else {
            boolean matched = false;
            for (ConditionalBlock cb : elseifBlocks) {
                if ((boolean) cb.getCondition().execute(symbolTable)) {
                    for (ASTNode n : cb.getBody()) n.execute(symbolTable);
                    matched = true;
                    break;
                }
            }
            if (!matched && elseBody != null) {
                for (ASTNode n : elseBody) n.execute(symbolTable);
            }
        }
        return null;
    }

    private boolean evaluate(ASTNode condition, Map<String, Object> symbolTable) {
        Object result = condition.execute(symbolTable);
        return result instanceof Boolean && (Boolean) result;
    }
}

