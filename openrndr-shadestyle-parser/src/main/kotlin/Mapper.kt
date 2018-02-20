package org.openrndr.shadestyle


import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.openrndr.shadestyle.ShadeStyleParser.*

interface ParseTreeToAstMapper<in PTN : ParserRuleContext, out ASTN : Node> {
    fun map(parseTreeNode: PTN) : ASTN
}


fun Token.startPoint() = Point(line, charPositionInLine)
fun Token.endPoint() = Point(line, charPositionInLine + if (type == EOF) 0 else text.length)

fun ParserRuleContext.toPosition() : Position {
    return Position(start.startPoint(), stop.endPoint())
}

fun ProgContext.toAst():Prog =
        Prog(this.statement_list().statement().map { it.toAst() }, toPosition())


fun StatementContext.position(): Position {
    return Position(start.startPoint(), stop.endPoint())
}

fun Type_qualifierContext.toAst() : TypeQualifier {

return TypeQualifier(this.text, this.toPosition())
}


fun Basic_typeContext.toAst():BasicType = when(this) {

    is VoidTypeContext -> VoidType(this.void_type().toPosition())
    is ScalaTypeContext -> ScalaType(this.scala_type().SCALA().symbol.text, this.toPosition())
    is VectorTypeContext -> this.vector_type().toAst()
    else -> TODO("${this.javaClass.canonicalName}")
}

private fun ShadeStyleParser.Vector_typeContext.toAst(): BasicType {

    return VectorType(this.VECTOR().text, this.toPosition())
}

fun Type_specifier_nonarrayContext.toAst(): TypeSpecifierNonArray =
    when(this) {
        is BasicTypeContext -> this.basic_type().toAst()
        is IdentifierContext -> Identifier(this.IDENTIFIER().text, this.toPosition())
        else -> TODO("${this.javaClass.canonicalName}")
    }

fun IntegerContext.toAst() : IntegerConstant = when(this) {
    is IntegerDecimalContext -> IntegerConstant( (this.DECIMAL().text).toInt(), this.toPosition())
    is IntegerHexContext -> IntegerConstant( (this.HEX().text).toInt(), this.toPosition())
    is IntegerOctalContext -> IntegerConstant( (this.OCTAL().text).toInt(), this.toPosition())
    else -> TODO("${this.javaClass.canonicalName}")
}


fun Float_numContext.toAst() : FloatConstant = FloatConstant(this.FLOAT_NUM().text.toDouble(), this.toPosition())
fun Bool_numContext.toAst() : BooleanConstant = BooleanConstant(this.text.toBoolean(), this.toPosition())

fun Constant_expressionContext.toAst():ConstantExpression = when(this) {
    is IntegerConstantExpressionContext -> this.integer().toAst()
    is FloatNumExpressionContext -> this.float_num().toAst()
    is BoolNumExpressionContext -> this.bool_num().toAst()
    else -> TODO("${this.javaClass.canonicalName}")
}

fun Struct_specifierContext.toAst():StructSpecifier =
    StructSpecifier(expression().toAst(), toPosition())

fun Array_struct_selectionContext.toAst():ArrayStructSelection =
        ArrayStructSelection(array_specifier().map { it.toAst() }, struct_specifier().map{ it.toAst()}, toPosition())

fun Primary_expressionContext.toAst():PrimaryExpression = when(this) {
    is ConstantExpressionContext -> this.constant_expression().toAst()
    is VectorExpressionContext -> VectorExpression(this.basic_type().toAst(), this.expression().map { it.toAst() }, toPosition())
    is PrimaryExpressionLeftValueContext -> LeftValueIndex(this.left_value().toAst(), this.array_struct_selection()?.toAst(), toPosition())
    else -> TODO("${this.javaClass.canonicalName}")
}

fun ExpressionContext.toAst(): Expression {
    return when(this) {
        is PrimaryExpressionContext -> this.primary_expression().toAst()
        is AddDivExpressionContext -> AddDivExpression(AddDivOp(this.ADDDIV_OP().text, this.toPosition()), this.expression().map {it.toAst()}, this.toPosition())
        is MulDivExpressionContext -> MulDivExpression(MulDivOp(this.MULDIV_OP().text, this.toPosition()), this.expression().map {it.toAst()}, this.toPosition())
        is ShiftOpExpressionContext -> ShiftOpExpression(ShiftOp(this.SHIFT_OP().text, this.toPosition()), this.expression().map {it.toAst()}, this.toPosition())
        is EqualOpExpressionContext -> EqualOpExpression(EqualOp(this.EQUAL_OP().text, this.toPosition()), this.expression().map {it.toAst()}, this.toPosition())
        is CompareOpExpressionContext -> CompareOpExpression(CompareOp(this.COMPARE_OP().text, this.toPosition()), this.expression().map { it.toAst() }, this.toPosition())
        is IncrementOpExpressionContext -> IncrementOpExpresssion(IncrementOp(this.INCREAMENT_OP().text, this.toPosition()), this.expression().toAst(), this.toPosition())
        else -> TODO("${this.javaClass.canonicalName}")
    }
}


fun Array_specifierContext.toAst():ArraySpecifier =
        ArraySpecifier(this.expression().toAst(), this.toPosition())

fun IdentifierContext.toAst():Identifier {
    return Identifier(this.text, this.toPosition())
}

fun Type_specifierContext.toAst(): TypeSpecifier {
    return TypeSpecifier(type_specifier_nonarray().toAst(),
    this.array_specifier().map { it.toAst() }, this.toPosition())
}

fun Function_callContext.toAst():FunctionCall =
        FunctionCall(
                this.function_name().IDENTIFIER().text,
                this.expression().map { it.toAst() },
                this.toPosition())

fun Left_valueContext.toAst():LeftValue = when(this) {
    is LeftValueFunctionCallContext -> this.function_call().toAst()
    is LeftValueExpressionContext -> Paren(this.expression().toAst(), this.toPosition())
    is LeftValueIdentifierContext -> Identifier(this.IDENTIFIER().text, this.toPosition())
    else -> TODO("${this.javaClass.canonicalName}")
}

fun Assignment_expressionContext.toAst():AssignmentExpression =
    AssignmentExpression(
            AssignmentOp(this.ASSIGNMENT_OP().text,
                    this.toPosition()),
            this.expression().toAst(), this.toPosition())

fun Simple_declaratorContext.toAst():SimpleDeclarator =
        SimpleDeclarator(
                this.left_value().toAst(),
                array_specifier().map { it.toAst() },
                assignment_expression()?.toAst(),
                toPosition())

fun Simple_declarationContext.toAst() : SimpleDeclaration  {
    return SimpleDeclaration(this.type_qualifier()?.toAst(), this.type_specifier().toAst(),
            this.simple_declarator().map { it.toAst() },
            this.toPosition())
}

fun Declaration_statementContext.toAst():DeclarationStatement = when(this) {
    is SimpleDeclarationContext -> this.simple_declaration().toAst()
    else -> TODO("${this.javaClass.canonicalName}")
}

fun Assignment_statementContext.toAst():AssignmentStatement = when(this) {
    is AssignmentStatementAssignExpressionContext -> NormalAssignmentStatement(this.left_value().toAst(), array_struct_selection()?.toAst(), assignment_expression().toAst(), toPosition())
    is AssignmentStateArithmeticAssignExpressionContext -> ArithmeticAssignmentStatement(this.left_value().toAst(), array_struct_selection()?.toAst(), arithmetic_assignment_expression().toAst(), toPosition())
    else -> TODO("${this.javaClass.canonicalName}")
}

private fun Arithmetic_assignment_expressionContext.toAst(): ArithmeticAssignmentExpression {

    return ArithmeticAssignmentExpression(ArithmeticAssignmentOp(this.ARITHMETIC_ASSIGNMENT_OP().text, this.toPosition()), this.expression().toAst(), toPosition())
}

fun Basic_statementContext.toAst():BasicStatement = when(this) {
    is AssignmentStatementContext -> this.assignment_statement().toAst()
    is DeclarationStatementContext -> this.declaration_statement().toAst()
    is ExpressionStatementContext -> this.expression_statement().toAst()
    else -> TODO("${this.javaClass.canonicalName}")
}

private fun Expression_statementContext.toAst(): BasicStatement {
    return ExpressionStatement(this.expression().toAst(), this.toPosition())

}


fun Selection_rest_statementContext.toAst():SelectionRestStatement = SelectionRestStatement(
        this.statement().map { it.toAst() }, this.toPosition()
)

fun Selection_statementContext.toAst():SelectionStatement = SelectionStatement(
        this.expression().toAst(), this.selection_rest_statement().toAst(), this.toPosition())

fun Return_TypeContext.toAst():ReturnType {
    return ReturnType(this.type_specifier().toAst(), this.toPosition())
}

fun Function_definitionContext.toAst():FunctionDefinition {

    return FunctionDefinition(
            this.return_Type().toAst(), this.function_name().text, this.func_decl_member().map { it.toAst() }, statement_list().statement().map { it.toAst() } , this.toPosition())
}

private fun ShadeStyleParser.Func_decl_memberContext.toAst(): FuncDeclMember {
    return FuncDeclMember(this.type_specifier().toAst(), this.IDENTIFIER().text, this.toPosition())
}

fun Simple_statementContext.toAst():SimpleStatement = when(this) {
    is BasicStatementContext ->  BasicStatementWithSemicolon((this.basic_statement()).toAst(), toPosition())
    is SelectionStatementContext -> this.selection_statement().toAst()
    is FunctionDefinitionStatementContext -> this.function_definition_statement().toAst()
    is IterationStatementContext -> this.iteration_statement().toAst()
    is JumpStatementContext -> this.jump_statement().toAst()

    else -> TODO("${this.javaClass.canonicalName}")
}



private fun Iteration_statementContext.toAst(): SimpleStatement = when (this) {

    is IterationWhileStatementContext -> WhileStatement(this.expression().toAst(), this.statement().toAst(), this.toPosition())
    is IterationDoStatementContext -> DoStatement(this.statement().toAst(), this.expression().toAst(), toPosition())
    is IterationForStatementContext -> ForStatement(this.for_init_statement().toAst(),
            this.for_cond_statement().toAst(), this.for_rest_statement().toAst(), this.statement_list().statement().map { it.toAst() }, this.toPosition())


    else -> TODO("${this.javaClass.canonicalName}")

}

private fun For_rest_statementContext.toAst(): ForRestStatement {
    return ForRestStatement(this.basic_statement().map { it.toAst() }, this.toPosition())
}

private fun For_init_statementContext.toAst(): ForInitStatement {
    return ForInitStatement(this.basic_statement().map {  it.toAst() }, this.toPosition())

}

private fun ShadeStyleParser.For_cond_statementContext.toAst(): ForCondStatement {
    return ForCondStatement(this.expression().toAst(), this.toPosition())
}

private fun Function_definition_statementContext.toAst(): SimpleStatement {
    return this.function_definition().toAst()
}


fun Compoud_statementContext.toAst():CompoundStatement =
        CompoundStatement(this.statement_list().statement().map { it.toAst() }, toPosition())

fun StatementContext.toAst() : Statement = when(this) {
    is SimpleStatementContext -> this.simple_statement().toAst()
    is CompoundStatementContext -> this.compoud_statement().toAst()
    is JumpStatementContext -> this.jump_statement().toAst()
    else -> TODO("${this.javaClass.canonicalName}")
}

private fun Jump_statementContext.toAst(): JumpStatement = when(this) {

    is ContinueStatementContext -> ContinueStatement(this.toPosition())
    is BreakStatementContext -> BreakStatement(this.toPosition())
    is ReturnStatementContext -> ReturnStatement(this.expression()?.toAst(), this.toPosition())
    else -> TODO("${this.javaClass.canonicalName}")

}
class ShadeStyleParseTreeToAstMapper : ParseTreeToAstMapper<ProgContext, Prog> {
    override fun map(parseTreeNode: ProgContext): Prog = parseTreeNode.toAst()
}