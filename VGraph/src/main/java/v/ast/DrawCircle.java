public class DrawCircle extends Draw {
    private final ASTNode x1, y1, r1;

    public DrawCircle(ASTNode x1, ASTNode y1, ASTNode r1) {
        this.x1 = x1;
        this.y1 = y1;
        this.r1 = r1;
    }
    @Override
    public Object execute(Context ctx) {
        int xStart = (Integer) x1.execute(ctx);
        int yStart = (Integer) y1.execute(ctx);
        int rEnd = (Integer) r1.execute(ctx);


        return null;

    }
}
