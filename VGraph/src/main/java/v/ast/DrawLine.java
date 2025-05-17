package v.ast;

import java.util.Map;

public class DrawLine extends Draw {
    private final ASTNode a,b,c,d;

    public DrawLine(ASTNode a, ASTNode b, ASTNode c,ASTNode d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
    @Override
    public Object execute(Map<String, Object> symbolTable) {
        int aStart = (Integer) a.execute(symbolTable);
        int bStart = (Integer) b.execute(symbolTable);
        int cStart= (Integer) c.execute(symbolTable);
        int dStart= (Integer) d.execute(symbolTable);


        return null;
    }
}
