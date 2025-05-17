package v.ast;

import java.util.Map;

public class ClearComm implements ASTNode{
    @Override
    public Object execute(Map<String, Object> symbolTable) {
        return "Clear()"; //así si puede interpretar que tiene que limpiar
        //Así si puede tener la referencia al canvas y borrarlo de la tabla
        /*Object canvasObj = symbolTable.get("canvas");
        if(canvasObj instanceof CanvasDibujo){ para esto habría que tener la clase de esto en lo que ya agarra la vara y lo limpia pero no sé bien como está guardado eso y hay que preguntarle a jose
            ((CanvasDibujo) canvasObj).clear();
            }
        return null;
        */
    }
}
