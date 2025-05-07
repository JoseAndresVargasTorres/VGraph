grammar VGraph;

@parser::header {
    import java.util.Map;
    import java.util.HashMap;
}

program:

    sentence
    {
        Map<String, Object> symbolTable = new HashMap<String,Object>();
        $sentence.node.execute(symbolTable);
    }
;
sentence returns [ASTNode node]:
    conditional {$node = $conditional.node;}
    | var_decl  {$node = $var_decl.node;}
    | var_assign {$node = $var_assign.node;}
    | setcolor {$node = $setcolor.node;}
    | draw {$node = $draw.node;}
    | shapeCall {$node = $shapeCall.node;};

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
    };

//declaracion de frame
frame returns [ASTNode node]:
     FRAME PAR_OPEN se=sentence PAR_CLOSE
          {
            $node = new Frame($se.node);
          };

//Declaracion de setcolor
setcolor returns [ASTNode node]:
     SETCOLOR PAR_OPEN t=color PAR_CLOSE
          {
            $node = new Setcolor($t.node);
          };

color returns [ASTNode node]:

     COLOR {$node = new Type($COLOR.text);};



//Declaracion de draw
draw returns [ASTNode node]:
    DRAW PAR_OPEN s=shapeCall PAR_CLOSE
    {
         $node = new shapeCall($s.node);
    };

shapeCall returns [ASTNode node]:
    LINE PAR_OPEN a=expression COMA b=expression COMA c=expression COMA d=expression PAR_CLOSE
        {
            $node = new DrawLine($a.node, $b.node, $c.node, $d.node);
        }
    |RECT PAR_OPEN x=expression COMA y=expression COMA w=expression COMA h=expression PAR_CLOSE
        {
            $node = new DrawRect($x.node, $y.node, $w.node, $h.node);
        }

    |CIRCLE PAR_OPEN x=expression COMA y=expression COMA r=expression PAR_CLOSE
        {
            $node = new DrawCircle($x.node, $y.node, $r.node);
        }

    | PIXEL PAR_OPEN x=expression COMA y=expression PAR_CLOSE
        {
            $node = new DrawPixel($x.node, $y.node);
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
    INT {$node = new Type($INT.text);};

//Asignacion de variables
var_assign returns [ASTNode node]:
    ID ASSIGN expression SEMICOLON
    {$node = new VarAssign($ID.text,$expression.node);};

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

//Comentarios
HASHTAG: '#';

//tipos
BOOLEAN: 'true' | 'false';
INT: 'int';
COLOR: 'rojo' | 'verde' | 'azul' | 'amarillo'| 'cyan'| 'magenta' | 'blanco' | 'marron ';


//Identificadores
ID: [a-zA-Z_][a-zA-Z0-9_]*;

NUMBER: [0-9]+;

WS: [ \t\n\r]+ -> skip;

