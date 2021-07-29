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
Predicate: '?';
Literal: '=';

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
    | LeftMustache? Predicate reference RightMustache?
    | LeftMustache? reference Predicate reference (':' reference)? RightMustache?
    | LeftMustache? Literal reference RightMustache?
    | LeftMustache? reference RightMustache?
    ;

reference
   : field
   | Subtract reference
   | reference Range reference
   | reference (Divide | Multiply) reference
   | reference (Add | Subtract) reference
   | reference Equal reference
   | reference Less reference
   | reference LessOrEqual reference
   | reference Greater reference
   | reference GreaterOrEqual reference
   | reference NotEqual reference
   | reference And reference
   | reference Or reference
   | Not reference
   | LeftParen reference RightParen
   ;

lambda
    : name Arrow reference
    | LeftParen (name (Comma name)*)* RightParen Arrow reference
    ;

//function
//    : name LeftParen (function_param (Comma function_param)*)? RightParen
//    ;

function_param
    : reference
    | lambda
    ;

field
   : True
   | False
   | Null
   | field (LeftBracket index RightBracket)
   | field LeftParen (function_param (Comma function_param)*)? RightParen
   | field ('.' name)
   | name
   | array
   | NUMBER
   | ESCAPED_STRING
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

ESCAPED_STRING : ('"' ( '\\"' | ~["] )* '"') | ('\'' ( '\\\'' | ~['] )* '\'');

STRING
   : [a-zA-Z_][a-zA-Z0-9_]*
   ;

NUMBER
   : [0-9]+('.'[0-9]+)?
   ;

WS
   : [ \r\n\t] -> channel(HIDDEN)
   ;