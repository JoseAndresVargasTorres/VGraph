package v.ast;
import java.util.Map;

public class DrawPixel extends Draw {
    private final ASTNode x,y;

    public DrawPixel(ASTNode x, ASTNode y) {
        this.x = x;
        this.y = y;
    }
    @Override
    public Object execute(Map<String, Object> symbolTable) {
        int xStart = (Integer) x.execute(symbolTable);
        int yStart = (Integer) y.execute(symbolTable);



        return null;
    }
}
