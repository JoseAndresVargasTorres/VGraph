public class DrawLine extends Draw {
    private final ASTNode a,b,c,d;

    public DrawLine(ASTNode a, ASTNode b, ASTNode c,ASTNode d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
    @Override
    public Object execute(Context ctx) {
        int aStart = (Integer) a.execute(ctx);
        int bStart = (Integer) b.execute(ctx);
        int cStart= (Integer) c.execute(ctx);
        int dStart= (Integer) d.execute(ctx);


        return null;
    }
}
