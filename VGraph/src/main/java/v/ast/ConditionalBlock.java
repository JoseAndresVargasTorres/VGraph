package v.ast;

import java.util.List;

public class ConditionalBlock {
    private ASTNode condition;
    private List<ASTNode> body;

    public ConditionalBlock(ASTNode condition, List<ASTNode> body) {
        this.condition = condition;
        this.body = body;
    }

    // getters
    public ASTNode getCondition() { return condition; }
    public List<ASTNode> getBody() { return body; }
}
