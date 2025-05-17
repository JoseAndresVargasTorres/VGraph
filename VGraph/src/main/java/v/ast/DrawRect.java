public class DrawRect extends Draw {
    private final ASTNode x,y,w,h;

    public DrawRect(ASTNode x, ASTNode y, ASTNode w,ASTNode h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    @Override
    public Object execute(Context ctx) {
        int xStart = (Integer) x.execute(ctx);
        int yStart = (Integer) y.execute(ctx);
        int wid= (Integer) w.execute(ctx);
        int hi= (Integer) h.execute(ctx);

        return null;
    }
}
