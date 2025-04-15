grammar VGraph;

start
:
	'hello' 'world'
;

WS
:
	[ \t\r\n]+ -> skip
;