package v.ast;
import java.util.List;
import java.util.Map;

public class Frame implements ASTNode {
    private final List<ASTNode> sentences;

    // Constructor que acepta una lista de sentencias
    public Frame(List<ASTNode> sentences) {
        this.sentences = sentences;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        if (sentences != null) {
            for (ASTNode sentence : sentences) {
                if (sentence != null) {
                    sentence.execute(symbolTable);
                }
            }
        }
        return null;
    }
}