package v.ast;

import java.util.List;
import java.util.Map;

public class Function implements ASTNode{
    public String name;
    private List<String> args;
    private List<ASTNode> sentences;

    public Function(String name, List<String> args, List<ASTNode> sentences) {
        this.name = name;
        this.args = args;
        this.sentences = sentences;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        symbolTable.put(name,this);
        return null;
    }

    public List<String> getArgs() {
        return args;
    }

    public List<ASTNode> getSentences() {
        return sentences;
    }
}

