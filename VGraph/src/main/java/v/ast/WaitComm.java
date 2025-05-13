package v.ast;

import java.util.Map;

public class WaitComm implements ASTNode{
    private final ASTNode value;

    public WaitComm(ASTNode value) {
        this.value = value;
    }

    @Override
    public Object execute(java.util.Map<String, Object> symbolTable) {
        Object result = value.execute(symbolTable);

        if(result instanceof Integer) {
            int waitTime = (int)result;
            try {
                Thread.sleep(waitTime);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        } else if (result instanceof Long) {
            long waitTime = (long)result;
            try {
                Thread.sleep(waitTime);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        } else
        {// CREO QUE LAS VARIABLES NO ESTÁN FUNCIONANDO, HAY QUE REVISAR ESO LUEGO
            throw new RuntimeException(result.getClass().getName() + " no es un numero");
        }
        return null;
    }
}
