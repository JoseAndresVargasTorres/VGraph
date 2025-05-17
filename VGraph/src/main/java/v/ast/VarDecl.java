package v.ast;

import java.awt.*;
import java.awt.Color;
import java.util.Map;

public class VarDecl implements ASTNode {
    private Map<String, ASTNode> decl_map;

    public VarDecl(Map<String, ASTNode> decl_map) {
        this.decl_map = decl_map;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        for (Map.Entry<String, ASTNode> entry : decl_map.entrySet()) {
            String varName = entry.getKey();
            ASTNode typeNode = entry.getValue();

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
        }

        return null;
    }
}