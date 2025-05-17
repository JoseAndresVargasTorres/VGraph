package v.ast;

import java.util.Map;

public class Type implements ASTNode{
    public String type;

    public Type(String type) {
        this.type = type;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        return type;
    }
}
