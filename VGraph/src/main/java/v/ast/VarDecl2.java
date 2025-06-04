package v.ast;

import java.util.Map;

public class VarDecl2 implements ASTNode {
    private ASTNode type;
    private Map<String, ASTNode> decl_map;

    // Constructor para declaraciones con asignación: (int) x = 5;
    public VarDecl2(ASTNode type, Map<String, ASTNode> decl_map) {
        this.type = type;
        this.decl_map = decl_map;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        String typeName = (String) type.execute(symbolTable);

        for (Map.Entry<String, ASTNode> entry : decl_map.entrySet()) {
            String varName = entry.getKey();
            ASTNode valueNode = entry.getValue();

            if (valueNode == null) {
                throw new RuntimeException("Error: VarDecl2 requiere un valor inicial para la variable: " + varName);
            }

            Object value = valueNode.execute(symbolTable);

            // Verificar y convertir tipos si es necesario
            switch (typeName) {
                case "int":
                    if (value instanceof Double) {
                        value = ((Double) value).intValue();
                    } else if (!(value instanceof Integer)) {
                        throw new RuntimeException("Error de tipo: Variable '" + varName +
                                "' de tipo int no puede ser asignada con " + value.getClass().getSimpleName());
                    }
                    break;
                case "color":
                    if (!(value instanceof vColor)) {
                        throw new RuntimeException("Error de tipo: Variable '" + varName +
                                "' de tipo color no puede ser asignada con " + value.getClass().getSimpleName());
                    }
                    break;
                default:
                    throw new RuntimeException("Tipo desconocido: " + typeName);
            }

            symbolTable.put(varName, value);
        }
        return null;
    }
}