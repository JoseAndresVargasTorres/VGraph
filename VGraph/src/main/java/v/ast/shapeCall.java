public class shapeCall extends ASTNode {
    private ASTNode shape;

    public shapeCall(ASTNode shape) {
        this.shape = shape;
    }
    @Override
    public Object execute(Context context) {
        return shape.execute(context);
    }
}
