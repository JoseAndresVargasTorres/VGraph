package v.ast;
import java.util.Map;

public class Frame implements ASTNode {
    private final ASTNode sentence;

    public Frame(ASTNode sentence) {
        this.sentence = sentence;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {

        if (sentence != null) {
            sentence.execute(symbolTable);
        }
        return null;
    }
}
