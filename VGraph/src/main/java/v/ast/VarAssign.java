package v.ast;

import java.util.Map;

public class VarAssign implements ASTNode{
    public String name;
    private ASTNode expression;

    public VarAssign(String name,ASTNode expression) {
        this.expression = expression;
        this.name = name;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        if(symbolTable.containsKey(name)){
            symbolTable.put(name,expression.execute(symbolTable));
        }
        //En el futuro que se tire error si no se encuentra el id
        return null;
    }
}
