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
        for (Map.Entry<String,ASTNode> entry : decl_map.entrySet()){
            switch ((String) entry.getValue().execute(symbolTable)){
                case "int":
                    symbolTable.put(entry.getKey(), 0);
                    break;
                case "color":
                    symbolTable.put(entry.getKey(),new Color());
                    break;
                default:
                    //En el futuro implementar tirar error, deberia ser uno de los tipos mencionados
                    symbolTable.put(entry.getKey(),new Object());
            }
        }
        return null;
    }
}
