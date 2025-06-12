package v.ast;

import java.util.Map;

public class VarDecl implements ASTNode {
    private ASTNode type;
    private Map<String, ASTNode> decl_map;

    public VarDecl(ASTNode type, Map<String, ASTNode> decl_map) {
        this.type = type;
        this.decl_map = decl_map;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        String typeName = (String) type.execute(symbolTable);

        for (Map.Entry<String, ASTNode> entry : decl_map.entrySet()) {
            String varName = entry.getKey();
            ASTNode valueNode = entry.getValue();

            Object defaultValue;

            if (valueNode == null) {
                // Sin valor inicial, usar valor por defecto según el tipo
                switch (typeName) {
                    case "int":
                        defaultValue = 0;
                        break;
                    case "color":
                        defaultValue = new vColor(); // color por defecto (negro)
                        break;
                    case "double":  // NUEVO CASO
                        defaultValue = 0.0;
                        break;
                    default:
                        throw new RuntimeException("Tipo desconocido: " + typeName);
                }
            } else {
                // Con valor inicial
                defaultValue = valueNode.execute(symbolTable);

                // Verificar compatibilidad de tipos
                if (typeName.equals("int") && defaultValue instanceof Double) {
                    defaultValue = ((Double) defaultValue).intValue();
                }
                // NUEVO: No convertir si el tipo es double
                // Si typeName es "double", mantener el valor como está
            }

            symbolTable.put(varName, defaultValue);
        }
        return null;
    }
}