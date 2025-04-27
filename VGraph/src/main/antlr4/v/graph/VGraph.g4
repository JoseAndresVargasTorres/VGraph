grammar VGraph;

@parser::header {
    import java.util.Map;
    import java.util.HashMap;
}

program:
    var_decl
    {
        Map<String, Object> symbolTable = new HashMap<String,Object>();
        $var_decl.node.execute(symbolTable);
    };

//Declaracion de variables
var_decl returns [ASTNode node]:
    PAR_OPEN type PAR_CLOSE id1=ID {
        Map<String,ASTNode> decl_map = new HashMap<String,ASTNode>();
        decl_map.put($id1.text,$type.node);
    }
    (COMA id2=ID {
        decl_map.put($id2.text,$type.node);
    }
    )*
    SEMICOLON
    {$node = new VarDecl(decl_map);};

type returns [ASTNode node]:
    INT {$node = new Type($INT.text);}
    | COLOR {$node = new Type($COLOR.text);};

//Asignacion de variables
var_assign returns [ASTNode node]:
    ID ASSIGN expression SEMICOLON
    {$node = new VarAssign($ID.text,$expression.node);};

//Operaciones
expression:
;
//Comentarios

//Funciones


//Palabras clave
DRAW: 'draw';
SETCOLOR: 'setcolor';
FRAME: 'frame';
LOOP: 'loop';
END: 'end';
WAIT: 'wait';
LINE: 'line';
CIRCLE: 'circle';
RECT: 'rect';
MOVE: 'move';
ANIMATE: 'animate';
COS: 'cos';
SIN: 'sin';
PIXEL: 'pixel';
IF: 'if';
ELSE: 'else';
PRINTLN: 'println';

//Operadores
PLUS: '+';
MINUS: '-';
MULT: '*';
DIV: '/';
MODULUS: '%';

//Comparadores
GT: '>';
LT: '<';
GEQ: '>=';
LEQ: '<=';
EQ: '==';
NEQ: '!=';
ASSIGN: '=';

//Delimitadores
BRACKET_OPEN: '{';
BRACKET_CLOSE: '}';
PAR_OPEN: '(';
PAR_CLOSE: ')';
QUATATION: '"';
COMA: ',';
DOT: '.';
SEMICOLON: ';';

//Comentarios
HASHTAG: '#';

//tipos
BOOLEAN: 'true' | 'false';
INT: 'int';
COLOR: 'color';


//Identificadores
ID: [a-zA-Z_][a-zA-Z0-9_]*;

NUMBER: [0-9]+;

WS: [ \t\n\r]+ -> skip;