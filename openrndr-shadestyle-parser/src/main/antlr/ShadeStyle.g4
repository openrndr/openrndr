// heavily based on https://github.com/labud/antlr4_convert/blob/master/src/GLSL/GLSL.g4
grammar ShadeStyle;

prog:
    statement_list;

//宏定义
preprocessor
    :   SHARP version_pre
    ;

version_pre
    : 'version' integer VERSOIN_PROFILE?
    ;

VERSOIN_PROFILE
    :   'core'
    |   'compatibility'
    |   'es'
    ;

//类型修饰语
type_qualifier
    :   (storage_qualifier
    |   layout_qualifier
    |   precision_qualifier
    |   interpolation_qualifier
    |   invariant_qualifier
    |   precise_qualifier)+
    ;


layout_qualifier:   'layout' LEFT_PAREN layout_qualifier_id  (COMMA layout_qualifier_id)* RIGHT_PAREN;
layout_qualifier_id: IDENTIFIER | IDENTIFIER ASSIGNMENT_OP constant_expression | 'shared';

storage_qualifier
    :   'const'
    |   'in'
    |   'out'
    |   'uniform'
    |   'buffer'
    |   'shared'
    ;

precision_qualifier
    :   'high_precision'
    |   'medium_precision'
    |   'low_precision'
    ;

interpolation_qualifier
    :   'smooth'
    |   'flat'
    |   'noperspective'
    ;

invariant_qualifier:    'invariant';

precise_qualifier:    'precise';

//元数据
integer: DECIMAL # integerDecimal
       | OCTAL   # integerOctal
       | HEX     # integerHex
       ;

float_num: FLOAT_NUM;

bool_num : 'true' | 'false';

//元数据类型
type_specifier: type_specifier_nonarray array_specifier*;

type_specifier_nonarray
    :   basic_type      # basicType
    |   IDENTIFIER      # identifier
    ;

array_specifier :   LEFT_BRACKET expression? RIGHT_BRACKET;
struct_specifier:   DOT expression;

basic_type
    :   void_type       # voidType
    |   scala_type      # scalaType
    |   vector_type     # vectorType
    |   matrix_type     # matrixType
    |   opaque_type     # opaqueType
    ;

void_type : 'void';

scala_type: SCALA;

vector_type: VECTOR;

matrix_type: MATRIX;

opaque_type
    :   float_opaque_type
    |   int_opaque_type
    |   u_int_opaque_type
    ;

float_opaque_type:  FLOAT_OPAQUE;
int_opaque_type: INT_OPAQUE;
u_int_opaque_type: U_INT_OPAQUE;


//表达式

expression
    :   primary_expression # primaryExpression
    |   expression INCREAMENT_OP # incrementOpExpression2
    |   INCREAMENT_OP expression # incrementOpExpression
    |   ADDDIV_OP expression # unaryAddDivOpExpression
    |   UNARY_OP expression # unaryOpExpression
    |   expression MULDIV_OP expression # mulDivExpression
    |   expression ADDDIV_OP expression # addDivExpression
    |   expression SHIFT_OP expression # shiftOpExpression
    |   expression COMPARE_OP expression # compareOpExpression
    |   expression EQUAL_OP expression # equalOpExpression
    |   expression BITWISE_OP expression # bitwiseOpExpression
    |   expression LOGIC_OP expression # logicOpExpression
    |   expression QUESTION expression COLON expression # ternaryExpression
    ;

primary_expression
    :   constant_expression # constantExpression
    |   basic_type LEFT_PAREN (expression  (COMMA expression)*)? RIGHT_PAREN # vectorExpression
    |   LEFT_PAREN type_specifier RIGHT_PAREN expression # castExpression
    |   left_value  array_struct_selection? # primaryExpressionLeftValue
    ;

constant_expression
    :   integer # integerConstantExpression
    |   float_num # floatNumExpression
    |   bool_num # boolNumExpression
    ;

left_value: function_call # leftValueFunctionCall
          | LEFT_PAREN expression RIGHT_PAREN # leftValueExpression
          | IDENTIFIER # leftValueIdentifier
          ;

array_struct_selection: (array_specifier | struct_specifier)+;

assignment_expression: ASSIGNMENT_OP expression;

arithmetic_assignment_expression: ARITHMETIC_ASSIGNMENT_OP expression;

//函数
function_definition
    : return_Type function_name
        LEFT_PAREN (func_decl_member  (COMMA func_decl_member)* )? RIGHT_PAREN LEFT_BRACE
            statement_list
        RIGHT_BRACE
    ;

function_declaration: return_Type function_name LEFT_PAREN (func_decl_member  (COMMA func_decl_member)* )?RIGHT_PAREN;

function_call: function_name LEFT_PAREN (expression (COMMA expression)*)? RIGHT_PAREN;

return_Type: type_specifier;

function_name: IDENTIFIER;

func_decl_member: type_specifier IDENTIFIER;

//语句(块)
statement_list: statement*;

statement
    : simple_statement # simpleStatement
    | compoud_statement # compoundStatement
    ;

simple_statement
    :   function_definition_statement # functionDefinitionStatement
    |   basic_statement SEMICOLON # basicStatement
    |   selection_statement # selectionStatement
    |   switch_statement # switchStatement
    |   case_label # caseLabel
    |   iteration_statement # iterationStatement
    |   jump_statement # jumpStatement
    ;

compoud_statement:  LEFT_BRACE statement_list RIGHT_BRACE;

basic_statement
    :   declaration_statement # declarationStatement
    |   assignment_statement # assignmentStatement
    |   expression_statement # expressionStatement
    ;

//声明语句(含初始化)
declaration_statement
    :   struct_declaration # structDeclaration
    |   simple_declaration # simpleDeclaration
    |   function_declaration # functionDeclaration
    ;

simple_declaration: type_qualifier? type_specifier  simple_declarator (COMMA simple_declarator)*;
simple_declarator: left_value array_specifier* (assignment_expression)?;

struct_declaration: type_qualifier? STRUCT IDENTIFIER LEFT_BRACE (simple_declaration SEMICOLON)+ RIGHT_BRACE;

//函数定义语句
function_definition_statement: function_definition;

//赋值语句
assignment_statement: left_value array_struct_selection? assignment_expression # assignmentStatementAssignExpression
                    | left_value array_struct_selection? arithmetic_assignment_expression # assignmentStateArithmeticAssignExpression
                    ;
//表达式语句
expression_statement: expression;

//条件选择语句
selection_statement:  IF LEFT_PAREN expression RIGHT_PAREN selection_rest_statement;
selection_rest_statement: statement (ELSE statement)? ;

//switch语句
switch_statement: SWITCH LEFT_PAREN expression RIGHT_PAREN LEFT_BRACE statement_list RIGHT_BRACE;

case_label
    : CASE expression COLON
    | DEFUALT COLON
    ;

//循环语句
iteration_statement
    :   WHILE LEFT_PAREN expression RIGHT_PAREN statement # iterationWhileStatement
    |   DO statement WHILE LEFT_PAREN expression RIGHT_PAREN SEMICOLON # iterationDoStatement
    |   FOR LEFT_PAREN for_init_statement for_cond_statement for_rest_statement RIGHT_PAREN LEFT_BRACE statement_list RIGHT_BRACE # iterationForStatement
    ;

for_init_statement
    :   (basic_statement (',' basic_statement)*)? SEMICOLON
    ;

for_cond_statement: expression SEMICOLON;

for_rest_statement: (basic_statement (',' basic_statement)*)? ;

//跳转语句
jump_statement
    :   CONTINUE SEMICOLON # continueStatement
    |   BREAK SEMICOLON # breakStatement
//    |   RETURN SEMICOLON # returnStatement
    |   RETURN expression? SEMICOLON # returnStatement
    ;

/**
 *词法
 */

STRUCT: 'struct';

IF: 'if';
ELSE: 'else';
QUESTION: '?';

FOR: 'for';
DO: 'do';
WHILE: 'while';


CONTINUE: 'continue';
BREAK: 'break';
RETURN: 'return';

SWITCH: 'switch';
CASE: 'case';
DEFUALT: 'defualt';

LEFT_PAREN: '(';
RIGHT_PAREN: ')';

LEFT_BRACE: '{';
RIGHT_BRACE: '}';

LEFT_BRACKET: '[';
RIGHT_BRACKET: ']';

DOT: '.';
COLON: ':';
SEMICOLON: ';';
COMMA: ',';
SHARP: '#';

//元数据
DECIMAL: [1-9]  DIGIT* INTEGER_SUFFIX?;
OCTAL: '0' OCTAL_DIGIT* INTEGER_SUFFIX?;
HEX:  ('0x' | '0X') HEX_DIGIT+ INTEGER_SUFFIX?;

FLOAT_NUM
    :   DIGIT+ DOT DIGIT* EXPONENT? FLOAT_SUFFIX?
    |   DOT DIGIT+ EXPONENT? FLOAT_SUFFIX?
    |   DIGIT+ EXPONENT FLOAT_SUFFIX?
    ;

//元数据类型
SCALA
    :   'bool'
    |   'int'
    |   'uint'
    |   'float'
    |   'double'
    ;

VECTOR: ('d'|'i'|'b'|'u')? 'vec' [2-4];

MATRIX: 'd'? 'mat'[2-4] ('x'[2-4])?;

FLOAT_OPAQUE: BASIC_OPAQUE_TYPE |
    ( 'sampler1DShadow' | 'sampler2DShadow' | 'sampler2DRectShadow'
    | 'sampler1DArrayShadow' | 'sampler2DArrayShadow' |
      'samplerCubeShadow' | 'samplerCubeArrayShadow');

INT_OPAQUE: 'i'BASIC_OPAQUE_TYPE;

U_INT_OPAQUE: 'u'BASIC_OPAQUE_TYPE | 'atomic_uint';

BASIC_OPAQUE_TYPE:  ('sampler' | 'image')
    ('1D'|'2D'|'3D'|'Cube'|'2DRect'|'1DArray'|'2DArray'|'Buffer'|'2DMS'|'2DMSArray'|'CubeArray');

//表达式
INCREAMENT_OP : '++' | '--';

UNARY_OP :  '~' | '!';

MULDIV_OP : '*' | '/' | '%';

ADDDIV_OP : '+' | '-';

SHIFT_OP : '<<' | '>>' ;

COMPARE_OP : '<' | '>' | '<=' | '>=';

EQUAL_OP:   '==' | '!=';

BITWISE_OP: '&' | '^' | '|';

LOGIC_OP:   '&&'| '^^' | '||';

ASSIGNMENT_OP: '=';

ARITHMETIC_ASSIGNMENT_OP
    :   MULDIV_OP ASSIGNMENT_OP
    |   ADDDIV_OP ASSIGNMENT_OP
    |   SHIFT_OP ASSIGNMENT_OP
    |   BITWISE_OP ASSIGNMENT_OP
    ;

/**
 *辅助词法
 */

fragment
DIGIT:  [0-9];

fragment
HEX_DIGIT : [0-9]| [a-f] | [A-F] ;

fragment
OCTAL_DIGIT : [0-7];

fragment
INTEGER_SUFFIX: 'u' | 'U';

fragment
EXPONENT : ('e'|'E') ADDDIV_OP? ('0'..'9')+ ;

fragment
FLOAT_SUFFIX: 'f' | 'F' | 'lf' | 'LF';


fragment
LETTER
    :   [a-z]
    |   [A-Z]
    |   '_'
    ;

IDENTIFIER
    :   LETTER (LETTER|DIGIT)*
    ;


//注释
COMMENT
    :   '/*' .*? '*/'    -> channel(HIDDEN) // match anything between /* and */
    ;
WS  :   [ \r\t\u000C\n]+ -> channel(HIDDEN)
    ;

LINE_COMMENT
    : '//' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN)
    ;