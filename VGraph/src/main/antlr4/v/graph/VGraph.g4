grammar VGraph;

@parser::header {
    import java.util.Map;
    import java.util.HashMap;
}

program:

    (sentence
    {
        Map<String, Object> symbolTable = new HashMap<String,Object>();
        $sentence.node.execute(symbolTable);
    })*
;
sentence returns [ASTNode node]:
    conditional {$node = $conditional.node;}
    | var_decl  {$node = $var_decl.node;}
    | var_assign {$node = $var_assign.node;}
    | wait_command {$node = $wait_command.node;}
    | comparison {$node = $comparison.node;}
    | loop_command {$node = $loop_command.node;}
    | println {$node = $println.node;};

wait_command returns [ASTNode node]:
    WAIT PAR_OPEN e=expression PAR_CLOSE SEMICOLON {$node = new WaitComm($e.node);}
;

clear_command returns [ASTNode node]:
CLEAR PAR_OPEN PAR_CLOSE SEMICOLON {$node = new ClearComm();};

println returns [ASTNode node]:
    PRINTLN expression SEMICOLON {$node = new Println($expression.node);}
    | PRINTLN clear_command SEMICOLON {$node = new Println($clear_command.node);}
    ;

loop_command returns [ASTNode node]
: LOOP PAR_OPEN e1=var_decl e2=comparison SEMICOLON e3=var_assign
    PAR_CLOSE BRACKET_OPEN e4=body BRACKET_CLOSE{$node = new LoopComm($e1.node,$e2.node,$e3.node,$e4.list);};

body returns [List<ASTNode> list]
@init {
    $list = new ArrayList<ASTNode>();
}
: (s=sentence { $list.add($s.node); })*;

comparison returns [ASTNode node]:
     e1=expression GT e2=expression {$node = new GreaterThan($e1.node,$e2.node);}
    | e1=expression LT e2=expression {$node = new LessThan($e1.node,$e2.node);}
    | e1=expression GEQ e2=expression {$node = new GreaterOrEqual($e1.node,$e2.node);}
    | e1=expression LEQ e2=expression {$node = new LessOrEqual($e1.node,$e2.node);}
    | e1=expression EQ e2=expression {$node = new Equal($e1.node,$e2.node);}
    | e1=expression NEQ e2=expression {$node = new NotEqual($e1.node,$e2.node);}
    ;
//*****************************************************************************************************************************************************************
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

//Declaracion de setcolor
//setcolor returns [ASTNode node]:;

//Declaracion de draw
//draw returns [ASTNode node]:;

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
CLEAR: 'clear';

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