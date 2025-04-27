package v.ast;

import java.awt.*;
import java.util.Map;

public class VarDecl implements ASTNode{
    private Map<String,ASTNode> decl_map;

    public VarDecl(Map<String,ASTNode> decl_map) {
        this.decl_map = decl_map;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        //Como puedo verificar los tipos?
        for (Map.Entry<String,ASTNode> entry : decl_map.entrySet()){
            symbolTable.put(entry.getKey(), new Object());
        }
        return null;
    }
}
