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
AsInt: '=';

Range: '..';
As: 'as';
Comma: ',';

LeftMustache: '{{';
RightMustache: '}}';

action
    : LeftMustache? Iteration reference (As name)? RightMustache?
    | LeftMustache? Predicate reference RightMustache?
    | LeftMustache? reference Predicate reference (':' reference)? RightMustache?
    | LeftMustache? AsInt reference RightMustache?
    | LeftMustache? reference RightMustache?
    ;

reference
   : field
   | function
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

function
    : name LeftParen (reference (Comma reference)*)? RightParen
    ;

field
   : field (LeftBracket index RightBracket)
   | field ('.' name)
   | name
   | array
   | NUMBER
   | ESCAPED_STRING
   ;

array
    : LeftBracket (field (Comma field)*)? RightBracket
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