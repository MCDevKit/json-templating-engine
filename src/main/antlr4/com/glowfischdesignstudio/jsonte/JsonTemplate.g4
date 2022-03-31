grammar JsonTemplate;

Less: '<';
LessOrEqual: '<=';
Equal: '==';
Greater: '>';
GreaterOrEqual: '>=';
NotEqual: '!=';
And: '&&';
Or: '||';
Not: '!';

//Math operators
Add: '+';
Subtract: '-';
Multiply: '*';
Divide: '/';

LeftParen: '(';
RightParen: ')';
LeftBracket: '[';
RightBracket: ']';

//Actions
Iteration: '#';
Question: '?';
Literal: '=';

NullCoalescing: '??';

Range: '..';
As: 'as';
Comma: ',';
Arrow: '=>';

LeftMustache: '{{';
RightMustache: '}}';

Null: 'null';
False: 'false';
True: 'true';

action
    : LeftMustache? Iteration reference (As name)? RightMustache?
    | LeftMustache? Question reference RightMustache?
    | LeftMustache? Literal reference RightMustache?
    | LeftMustache? reference RightMustache?
    ;

reference
   : field
   | reference Equal reference
   | reference Less reference
   | reference LessOrEqual reference
   | reference Greater reference
   | reference GreaterOrEqual reference
   | reference NotEqual reference
   | reference And reference
   | reference Or reference
   | Not reference
   | reference Question reference (':' reference)?
   | LeftParen reference RightParen
   ;

lambda
    : name Arrow reference
    | LeftParen (name (Comma name)*)* RightParen Arrow reference
    ;

function_param
    : reference
    | lambda
    ;

field
   : LeftParen field RightParen
   | True
   | False
   | Null
   | NUMBER
   | ESCAPED_STRING
   | array
   | name
   | field (Question? '.' name)
   | field (Question? LeftBracket index RightBracket)
   | field LeftParen (function_param (Comma function_param)*)? RightParen
   | Subtract field
   | field (Divide | Multiply) field
   | field (Add | Subtract) field
   | field Range field
   | field NullCoalescing field
   ;

array
    : LeftBracket (reference (Comma reference)*)? RightBracket
    ;

name
   : STRING
   ;

index
   : reference
   | NUMBER
   | ESCAPED_STRING
   ;

ESCAPED_STRING : ('"' ('\\' . | ~["\\])* '"') | ('\'' ('\\' . | ~['\\])* '\'');

STRING
   : [a-zA-Z_][a-zA-Z0-9_]*
   ;

NUMBER
   : [0-9]+('.'[0-9]+)?
   ;

WS
   : [ \r\n\t] -> channel(HIDDEN)
   ;