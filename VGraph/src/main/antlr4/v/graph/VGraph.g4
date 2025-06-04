grammar VGraph;

@parser::header {
    import java.util.Map;
    import java.util.HashMap;
    import v.ast.*;
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

sentence returns [ASTNode node]:
    conditional {$node = $conditional.node;}
    | var_decl  {$node = $var_decl.node;}
    | var_assign {$node = $var_assign.node;}
    | setcolor {$node = $setcolor.node;}
    | draw {$node = $draw.node;}
    | shapeCall {$node = $shapeCall.node;}
    | function {$node = $function.node;}
    | funCall {$node = $funCall.node;}
    | println {$node = $println.node;}
    | loop_command {$node = $loop_command.node;}
    | wait_command {$node = $wait_command.node;}
    | frame {$node = $frame.node;}
    | clear_command {$node = $clear_command.node;}
;

println returns [ASTNode node]:
    PRINTLN expression SEMICOLON
    {$node = new Println($expression.node);}
;

wait_command returns [ASTNode node]:
    WAIT PAR_OPEN e=expression PAR_CLOSE SEMICOLON {$node = new WaitComm($e.node);}
;

clear_command returns [ASTNode node]:
    CLEAR PAR_OPEN PAR_CLOSE SEMICOLON {$node = new ClearComm();}
;

loop_command returns [ASTNode node]:
    LOOP PAR_OPEN e1=var_assign e2=comparison SEMICOLON e3=increment_loop
    PAR_CLOSE BRACKET_OPEN e4=body BRACKET_CLOSE{$node = new LoopComm($e1.node,$e2.node,$e3.node,$e4.list);}
;

body returns [List<ASTNode> list]:
{
    $list = new ArrayList<ASTNode>();
}
 (s=sentence { $list.add($s.node); })*
;

increment_loop returns [ASTNode node]:
    ID ASSIGN expression
    {$node = new VarAssign($ID.text,$expression.node);}
;

comparison returns [ASTNode node]:
     e1=operand GT e2=operand {$node = new GreaterThan($e1.node,$e2.node);}
    | e1=operand LT e2=operand {$node = new LessThan($e1.node,$e2.node);}
    | e1=operand GEQ e2=operand {$node = new GreaterOrEqual($e1.node,$e2.node);}
    | e1=operand LEQ e2=operand {$node = new LessOrEqual($e1.node,$e2.node);}
    | e1=operand EQ e2=operand {$node = new Equal($e1.node,$e2.node);}
    | e1=operand NEQ e2=operand {$node = new NotEqual($e1.node,$e2.node);}
;

conditional returns [ASTNode node]:
    IF PAR_OPEN cond=expression PAR_CLOSE
    {
        List<ASTNode> ifBody = new ArrayList<>();
    }
    BRACKET_OPEN (s=sentence {ifBody.add($s.node);})* BRACKET_CLOSE

    {
        List<ConditionalBlock> elseifBlocks = new ArrayList<>();
        List<ASTNode> elseBody = null;
    }

    (   ELSEIF PAR_OPEN elseifCond=expression PAR_CLOSE
        {
            List<ASTNode> elseifBody = new ArrayList<>();
        }
        BRACKET_OPEN (s1=sentence {elseifBody.add($s1.node);})* BRACKET_CLOSE
        {
            elseifBlocks.add(new ConditionalBlock($elseifCond.node, elseifBody));
        }
    )*

    (   ELSE
        {
            elseBody = new ArrayList<>();
        }
        BRACKET_OPEN (s2=sentence {elseBody.add($s2.node);})* BRACKET_CLOSE
    )?
    {
        $node = new If($cond.node, ifBody, elseifBlocks, elseBody);
    }
;

frame returns [ASTNode node]:
     FRAME BRACKET_OPEN
     {
         List<ASTNode> frameBody = new ArrayList<>();
     }
     (se=sentence { frameBody.add($se.node); })*
     BRACKET_CLOSE
     {
         $node = new Frame(frameBody);
     }
;

setcolor returns [ASTNode node]:
     SETCOLOR PAR_OPEN t=expression PAR_CLOSE SEMICOLON
          {
            $node = new Setcolor($t.node);
          }
;

draw returns [ASTNode node]:
    DRAW s=shapeCall SEMICOLON
    {
         $node = new shapeCall($s.node);
    }
;

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
        }
;

function returns [ASTNode node]:
    FUNCTION funID=ID
        {
           List<String> args = new ArrayList<String>();
           List<ASTNode> sentences = new ArrayList<ASTNode>();
        }
        PAR_OPEN
            (
                arg1=ID {args.add($arg1.text);}
                (COMA arg2=ID {args.add($arg2.text);})*
            )?
        PAR_CLOSE
        BRACKET_OPEN
            s1=sentence {sentences.add($s1.node);}
            (s2=sentence {sentences.add($s2.node);})*
        BRACKET_CLOSE
        {
            $node = new Function($funID.text,args,sentences);
        }
;

funCall returns [ASTNode node]:
    funID=ID
    {
        List<ASTNode> args = new ArrayList<ASTNode>();
    }
    PAR_OPEN
        (
            arg1=expression {args.add($arg1.node);}
            (COMA arg2=expression {args.add($arg2.node);})*
        )?
    PAR_CLOSE
    {
        $node = new FunctionCall($funID.text,args);
    }
    SEMICOLON
;

// CORREGIDA: Declaración de variables con labels correctos
var_decl returns [ASTNode node]:
    // Caso 1: declaración simple: (int) x, y, t;
    PAR_OPEN type PAR_CLOSE id1=ID {
        Map<String, ASTNode> decl_map = new HashMap<>();
        decl_map.put($id1.text, null);
    }
    (COMA id2=ID {
        decl_map.put($id2.text, null);
    })*
    SEMICOLON
    {
        $node = new VarDecl($type.node, decl_map);
    }

    // Caso 2: declaración con asignación: (int) x = -1.5;
    | PAR_OPEN type PAR_CLOSE id1=ID ASSIGN expr=expression SEMICOLON
    {
        Map<String, ASTNode> decl_map = new HashMap<>();
        decl_map.put($id1.text, $expr.node);
        $node = new VarDecl2($type.node, decl_map);
    }
;

type returns [ASTNode node]:
    INT {$node = new Type($INT.text);}
    | COLOR {$node = new Type($COLOR.text);}
;

var_assign returns [ASTNode node, Token id, ASTNode value]:
    idTok=ID ASSIGN expr=expression SEMICOLON
    {
        $node = new VarAssign($idTok.text, $expr.node);
        $id = $idTok;
        $value = $expr.node;
    }
;

expression returns [ASTNode node]:
    operand {$node = $operand.node;}
    | comparison {$node = $comparison.node;}
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
    NUMBER {
        if($NUMBER.text.contains(".")){$node = new Constant(Double.parseDouble($NUMBER.text));}
        else {$node = new Constant(Integer.parseInt($NUMBER.text));}
    }
    | COLOR_VALUES {$node = new Constant(new vColor($COLOR_VALUES.text));}
    | BOOLEAN {$node = new Constant(Boolean.parseBoolean($BOOLEAN.text));}
    | ID {$node = new VarRef($ID.text);}
    | PAR_OPEN expression {$node = $expression.node;} PAR_CLOSE
    | cos {$node = $cos.node;}
    | sin {$node = $sin.node;}
;

// Tokens
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
FUNCTION: 'function';
CLEAR: 'clear';
RETURN: 'return';

PLUS: '+';
MINUS: '-';
MULT: '*';
DIV: '/';
MODULUS: '%';

GT: '>';
LT: '<';
GEQ: '>=';
LEQ: '<=';
EQ: '==';
NEQ: '!=';
ASSIGN: '=';

BRACKET_OPEN: '{';
BRACKET_CLOSE: '}';
PAR_OPEN: '(';
PAR_CLOSE: ')';
QUATATION: '"';
COMA: ',';
DOT: '.';
SEMICOLON: ';';

BOOLEAN: 'true' | 'false';
INT: 'int';
COLOR: 'color';
COLOR_VALUES:'negro'| 'blanco'| 'rojo'| 'verde'| 'azul'| 'amarillo'| 'cyan'| 'magenta'| 'marron';

HASHTAG_COMMENT: '#' ~[\r\n]* -> skip;

ID: [a-zA-Z_][a-zA-Z0-9_]*;
NUMBER: '-'? [0-9]+ ('.' [0-9]+)?;

WS: [ \t\n\r]+ -> skip;