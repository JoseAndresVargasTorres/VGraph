package v.ast;
import java.util.Map;

public class Setcolor implements ASTNode{
    private ASTNode color;

    public Setcolor(ASTNode color) {
        this.color = color;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        return null;
    }
}
