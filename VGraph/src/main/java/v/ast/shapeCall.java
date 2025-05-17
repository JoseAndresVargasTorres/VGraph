package v.ast;
import java.util.Map;

public class shapeCall implements ASTNode {
    private ASTNode shape;

    public shapeCall(ASTNode shape) {
        this.shape = shape;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        return null;
    }
}
