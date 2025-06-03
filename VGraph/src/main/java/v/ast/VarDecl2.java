package v.ast;

import java.util.Map;

public class VarDecl2 implements ASTNode {
    private Map<String, ASTNode> decl_map;
    private ASTNode type;

    public VarDecl2(ASTNode type, Map<String, ASTNode> decl_map) {
        this.decl_map = decl_map;
        this.type = type;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        for (Map.Entry<String, ASTNode> entry : decl_map.entrySet()) {
            String varName = entry.getKey();
            ASTNode typeNode = entry.getValue();
            boolean consistent = false;
            String type = (String) this.type.execute(symbolTable);
            String currentType = typeNode.execute(symbolTable).getClass().getSimpleName();
            if(type.equals("int") && currentType.equals("Integer")){
                consistent = true;
            } else if (type.equals("int") && currentType.equals("Double")){
               Double value = (Double) typeNode.execute(symbolTable);
               symbolTable.put(varName,((Double) typeNode.execute(symbolTable)).intValue());
            } else if (type.equals("color") && currentType.equals("vColor")){
                consistent = true;
            }
            if(consistent){
                symbolTable.put(varName,typeNode.execute(symbolTable));
            }
        }
        return null;
    }
}