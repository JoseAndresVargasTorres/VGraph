grammar VGraph;

@parser::header {
    import java.util.Map;
    import java.util.HashMap;
}

program:
    s1=sentence
    {
        List<ASTNode> runBody = new ArrayList<ASTNode>();
        runBody.add($s1.node);

    }
    (s2=sentence  {runBody.add($s2.node);} )*
    {
        Map<String, Object> symbolTable = new HashMap<String,Object>();
        for(ASTNode n : runBody){
            n.execute(symbolTable);
        }
    }
;

//Sentencias: frames, loops, funciones, Ifs, declaraciones, asignaciones
sentence returns [ASTNode node]:
    conditional {$node = $conditional.node;}
    | var_decl  {$node = $var_decl.node;}
    | var_assign {$node = $var_assign.node;}
;

//Ifs
conditional returns [ASTNode node]:
    IF PAR_OPEN expression PAR_CLOSE
    {
        List<ASTNode> body = new ArrayList<ASTNode>();
    }
    BRACKET_OPEN (s1=sentence {body.add($s1.node);})* BRACKET_CLOSE

    ELSEIF
    {
        List<ASTNode> elseifbody = new ArrayList<ASTNode>();
    }
    BRACKET_OPEN (s2=sentence {elseifbody.add($s2.node);})* BRACKET_CLOSE

    ELSE
    {
        List<ASTNode> elseBody = new ArrayList<ASTNode>();
    }
    BRACKET_OPEN (s3=sentence {elseBody.add($s3.node);})* BRACKET_CLOSE
    {
        $node = new If($expression.node,body,elseifbody,elseBody);
    }
;

//Declaracion de setcolor
//setcolor returns [ASTNode node]:;

//Declaracion de draw
//draw returns [ASTNode node]:;

//Funciones

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
    {$node = new VarDecl(decl_map);}
;

type returns [ASTNode node]:
    INT {$node = new Type($INT.text);}
    | COLOR {$node = new Type($COLOR.text);}
;

//Asignacion de variables
var_assign returns [ASTNode node]:
    ID ASSIGN expression SEMICOLON
    {$node = new VarAssign($ID.text,$expression.node);}
;

//Operaciones
expression returns [ASTNode node]:
    operand {$node = $operand.node;}
;

operand returns [ASTNode node]:
    t1=factor {$node = $t1.node;}
    (
        PLUS t2=factor {$node = new Addition($node,$t2.node);}
        | MINUS t2=factor {$node = new Subtraction($node,$t2.node);}
    )*
;

factor returns [ASTNode node]:
    t1=term {$node = $t1.node;}
    (
        MULT t2=term {$node = new Multiplication($node,$t2.node);}
        | DIV  t2=term {$node = new Division($node,$t2.node);}
        | MODULUS t2=term {$node = new Modulus($node,$t2.node);}
    )*
;

sin returns [ASTNode node]:
    SIN PAR_OPEN expression PAR_CLOSE
    {$node = new Sin($expression.node);}
;

cos returns [ASTNode node]:
    COS PAR_OPEN expression PAR_CLOSE
    {$node = new Cos($expression.node);}
;

term returns [ASTNode node]:
    NUMBER {$node = new Constant(Integer.parseInt($NUMBER.text));}
    | BOOLEAN {$node = new Constant(Boolean.parseBoolean($BOOLEAN.text));}
    | ID {$node = new VarRef($ID.text);}
    | PAR_OPEN expression {$node = $expression.node;} PAR_CLOSE
    | cos {$node = $cos.node;}
    | sin {$node = $sin.node;}
;

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
ELSEIF: 'elseif';
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

//tipos
BOOLEAN: 'true' | 'false';
INT: 'int';
COLOR: 'color';

//Comentarios
HASHTAG_COMMENT: '#' ~[\r\n]* -> skip;

//Identificadores
ID: [a-zA-Z_][a-zA-Z0-9_]*;

NUMBER: [0-9]+;

WS: [ \t\n\r]+ -> skip;