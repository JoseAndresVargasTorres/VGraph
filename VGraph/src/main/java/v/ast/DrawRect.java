package v.ast;
import java.util.Map;

public class DrawRect extends Draw {
    private final ASTNode x,y,w,h;

    public DrawRect(ASTNode x, ASTNode y, ASTNode w,ASTNode h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    @Override
    public Object execute(Map<String, Object> symbolTable) {
        int xStart = (Integer) x.execute(symbolTable);
        int yStart = (Integer) y.execute(symbolTable);
        int wid= (Integer) w.execute(symbolTable);
        int hi= (Integer) h.execute(symbolTable);

        return null;
    }
}
