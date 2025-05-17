package v.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionCall implements ASTNode{
    public String name;
    private List<ASTNode> args;

    public FunctionCall(String name, List<ASTNode> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public Object execute(Map<String, Object> symbolTable) {
        if (!symbolTable.containsKey(name)) {
            throw new RuntimeException("Error: Funcion '" + name + "' no ha sido declarada.");
        }

        Object value = symbolTable.get(name);
        if (!value.getClass().getSimpleName().equals("Function")){
            throw new RuntimeException("Error:  '" + name + "' no es una funcion");
        }
        Function fun = (Function) value;
        List<ASTNode>  sentences = fun.getSentences();
        List<String> args = fun.getArgs();

        if (this.args.size() != args.size()){
            throw new RuntimeException("Error: '" + name + "' esperaba " + args.size() + " argumentos, pero se le dieron " + this.args.size());
        }

        Map<String, Object> localSymbolTable = new HashMap<>(symbolTable);
        for (int i = 0; i < args.size() ; i++) {
            localSymbolTable.put(args.get(i), this.args.get(i).execute(symbolTable));
        }
        for (ASTNode sentence: sentences){
            sentence.execute(localSymbolTable);
        }
        return null;
    }
}

