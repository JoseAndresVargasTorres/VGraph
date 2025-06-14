package v.ast;


import java.util.Map;

public abstract class Draw implements ASTNode {

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        return null;
    }
}