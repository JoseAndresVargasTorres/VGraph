package v.ast;

import java.util.Map;

public class Sin implements ASTNode{
    public ASTNode expression;

    public Sin(ASTNode expression) {
        this.expression = expression;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        Object value = expression.execute(symbolTable);
        String valueType = value.getClass().getSimpleName();
        if (valueType.equals("Integer")){
            value = ((Integer) value).doubleValue();
        } else if (!valueType.equals("Double")) {
            throw new RuntimeException(
                    "Error seno: se intengo ingresar variable tipo" + valueType + "pero se esperaba Double"
            );
        }

        return Math.sin((double) value);
    }
}
