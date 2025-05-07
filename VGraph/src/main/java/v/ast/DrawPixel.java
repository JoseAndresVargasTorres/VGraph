public class DrawPixel extends Draw {
    private final ASTNode x,y;

    public DrawPixel(ASTNode x, ASTNode y) {
        this.x = x;
        this.y = y;
    }
    @Override
    public Object execute(Context ctx) {
        int xStart = (Integer) x.execute(ctx);
        int yStart = (Integer) y.execute(ctx);



        return null;
    }
}
