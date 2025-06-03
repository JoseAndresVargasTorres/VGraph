package v.ast;

import java.awt.*;
import java.awt.Color;
import java.util.Map;

public class VarDecl implements ASTNode {
    private Map<String, ASTNode> decl_map;
    private ASTNode type;

    public VarDecl(Map<String, ASTNode> decl_map) {
        this.decl_map = decl_map;
        this.type = null;
    }

    public VarDecl(ASTNode type, Map<String, ASTNode> decl_map) {
        this.decl_map = decl_map;
        this.type = type;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        for (Map.Entry<String, ASTNode> entry : decl_map.entrySet()) {
            String varName = entry.getKey();
            ASTNode typeNode = entry.getValue();
            if(this.type == null){
                // Ejecutamos el nodo de tipo para obtener el nombre del tipo
                String typeName = (String) typeNode.execute(symbolTable);
                Object defaultValue;

                // Creamos el valor por defecto según el tipo
                switch (typeName) {
                    case "int":
                        defaultValue = 0;
                        break;
                    case "color":
                        defaultValue = new vColor(); // o cualquier color por defecto
                        break;
                    default:
                        throw new RuntimeException("Tipo desconocido: " + typeName);
                }

                symbolTable.put(varName, defaultValue);
            } else {
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
        }
        return null;
    }
}