public class Frame extends ASTNode {
    private final ASTNode sentence;

    public Frame(ASTNode sentence) {
        this.sentence = sentence;
    }

    @Override
    public Object execute(Context ctx) {

        if (sentence != null) {
            sentence.execute(ctx);
        }
        return null;
    }
}
