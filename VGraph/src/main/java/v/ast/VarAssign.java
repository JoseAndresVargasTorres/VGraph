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
        if (!symbolTable.containsKey(name)) {
            throw new RuntimeException("Error: Variable '" + name + "' no ha sido declarada.");
        }

        Object currentValue = symbolTable.get(name);
        Object newValue = expression.execute(symbolTable);

        String currentType =  currentValue.getClass().getSimpleName();
        String newType = newValue.getClass().getSimpleName();
        //System.out.println("Current:" + currentValue.getClass().getName());
        //System.out.println("New:"+newValue.getClass().getName());

        //Se verifica que lo que se este asignando este en congruencia con el tipo
        if (!currentType.equals(newType)) {
            if(currentType.equals("Integer") && newType.equals("Double")){
                newValue = ((Double) newValue).intValue();
                //System.out.println(newValue.toString() + newValue.getClass().getSimpleName());
                symbolTable.put(name, newValue);
            } else {
                throw new RuntimeException("Error de tipo: Variable '" + name +
                        "' esperaba un valor de tipo " + currentType +
                        " pero se intentó asignar " + newType);
            }
        }

        symbolTable.put(name, newValue);
        return null;
    }

}
