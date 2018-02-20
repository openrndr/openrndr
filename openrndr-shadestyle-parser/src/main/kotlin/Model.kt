package org.openrndr.shadestyle

import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

// heavily based on https://github.com/ftomassetti/LangSandbox/blob/master/src/main/kotlin/me/tomassetti/sandy/ast/Model.

interface Node {
    val position: Position?
}

fun Node.isBefore(other: Node) : Boolean = position!!.start.isBefore(other.position!!.start)

fun Point.isBefore(other: Point) : Boolean = line < other.line || (line == other.line && column < other.column)

data class Point(val line: Int, val column: Int) {
    override fun toString() = "Line $line, Column $column"
}

data class Position(val start: Point, val end: Point)

fun pos(startLine:Int, startCol:Int, endLine:Int, endCol:Int) = Position(Point(startLine,startCol),Point(endLine,endCol))

fun Node.process(operation: (Node) -> Unit) {
    operation(this)
    this.javaClass.kotlin.memberProperties.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> v.process(operation)
            is Collection<*> -> v.forEach { if (it is Node) it.process(operation) }
        }
    }
}

fun <T: Node> Node.specificProcess(klass: Class<T>, operation: (T) -> Unit) {
    process { if (klass.isInstance(it)) { operation(it as T) } }
}

fun Node.transform(operation: (Node) -> Node) : Node {
    operation(this)
    val changes = HashMap<String, Any>()
    this.javaClass.kotlin.memberProperties.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> {
                val newValue = v.transform(operation)
                if (newValue != v) changes[p.name] = newValue
            }
            is Collection<*> -> {
                val newValue = v.map { if (it is Node) it.transform(operation) else it }
                if (newValue != v) changes[p.name] = newValue
            }
        }
    }
    var instanceToTransform = this
    if (!changes.isEmpty()) {
        val constructor = this.javaClass.kotlin.primaryConstructor!!
        val params = HashMap<KParameter, Any?>()
        constructor.parameters.forEach { param ->
            if (changes.containsKey(param.name)) {
                params[param] = changes[param.name]
            } else {
                params[param] = this.javaClass.kotlin.memberProperties.find { param.name == it.name }!!.get(this)
            }
        }
        instanceToTransform = constructor.callBy(params)
    }
    return operation(instanceToTransform)
}

data class Prog(val statements: List<Statement>,
                          override val position: Position) : Node

interface Statement:Node {}
interface Expression:LeftValue
interface Type:Node{}


data class AddDivOp(val op:String, override val position:Position):Node
data class AddDivExpression(val op:AddDivOp, val expressions: List<Expression>,
                            override val position: Position):Expression

data class MulDivOp(val op:String, override val position:Position):Node
data class MulDivExpression(val op:MulDivOp, val expressions: List<Expression>,
                            override val position: Position):Expression


data class ShiftOp(val op:String, override val position:Position):Node
data class ShiftOpExpression(val op:ShiftOp, val expressions: List<Expression>,
                            override val position: Position):Expression


data class EqualOp(val op:String, override val position:Position):Node
data class EqualOpExpression(val op:EqualOp, val expressions: List<Expression>,
                             override val position: Position):Expression

data class CompareOp(val op:String, override val position:Position):Node
data class CompareOpExpression(val op:CompareOp, val expressions: List<Expression>,
                             override val position: Position):Expression

data class IncrementOp(val op:String, override val position:Position):Node
data class IncrementOpExpresssion(val op:IncrementOp, val expressions: Expression,
                                  override val position: Position):Expression

interface SimpleStatement: Statement

data class CompoundStatement(val statements:List<Statement>, override val position: Position):Statement


interface BasicStatement: SimpleStatement
data class BasicStatementWithSemicolon(val basicStatement:BasicStatement, override val position: Position): SimpleStatement
interface DeclarationStatement: BasicStatement
interface IterationStatement: SimpleStatement

data class WhileStatement(val expression: Expression, val statement: Statement, override val position: Position) : IterationStatement
data class DoStatement(val statement:Statement, val expression: Expression, override val position: Position): IterationStatement
data class ForStatement(val initStatement: ForInitStatement, val condStatement: ForCondStatement, val restStatment: ForRestStatement, val statements: List<Statement>, override val position: Position):IterationStatement

data class ForInitStatement(val basicStatements:List<BasicStatement>, override val position: Position):Node
data class ForCondStatement(val expression: Expression, override val position: Position):Node
data class ForRestStatement(val basicStatements: List<BasicStatement>, override val position:Position):Node


interface JumpStatement:Statement, SimpleStatement
data class ContinueStatement(override val position: Position) : JumpStatement
data class BreakStatement(override val position: Position): JumpStatement
data class ReturnStatement(val expression: Expression?, override val position: Position) : JumpStatement

interface AssignmentStatement: BasicStatement
data class ExpressionStatement(val expression:Expression, override val position:Position): BasicStatement

data class NormalAssignmentStatement(val leftValue:LeftValue, val arrayStructSelection: ArrayStructSelection?, val assignmentExpression:AssignmentExpression, override val position: Position) : AssignmentStatement
data class ArithmeticAssignmentStatement(val leftValue:LeftValue, val arrayStructSelection: ArrayStructSelection?, val assignmentExpression:ArithmeticAssignmentExpression, override val position: Position) : AssignmentStatement

interface PrimaryExpression: Expression

interface ConstantExpression : PrimaryExpression
data class LeftValueIndex(val leftValue: LeftValue, val arrayStructSelection: ArrayStructSelection?, override val position: Position): PrimaryExpression
data class VectorExpression(val basicType: BasicType, val expressions:List<Expression>, override val position:Position):PrimaryExpression

data class IntegerConstant(val value:Int, override val position: Position):ConstantExpression
data class FloatConstant(val value:Double, override val position: Position):ConstantExpression
data class BooleanConstant(val value:Boolean, override val position: Position):ConstantExpression

data class TypeQualifier(val qualifier:String, override val position: Position): Node
data class TypeSpecifier(val typeSpecifierNonArray: TypeSpecifierNonArray,
                         val arraySpecifiers: List<ArraySpecifier>,
                         override val position: Position): Node


data class ArraySpecifier(val expression:Expression, override val position:Position) :Node
data class StructSpecifier(val expression:Expression, override val position:Position) : Node
data class ArrayStructSelection(val arraySpecifiers: List<ArraySpecifier>,
                                val structSpecifiers:List<StructSpecifier>,
                                override val position: Position):PrimaryExpression

interface BasicType: TypeSpecifierNonArray
interface TypeSpecifierNonArray: Node

data class VoidType(override val position:Position): BasicType
data class ScalaType(val type:String, override val position:Position): BasicType
data class VectorType(val type:String, override val position:Position): BasicType
data class MatrixType(override val position:Position): BasicType
data class OpaqueType(override val position:Position): BasicType


data class Identifier(val id:String, override val position: Position):TypeSpecifierNonArray, LeftValue
data class Paren(val expression: Expression, override val position: Position):LeftValue

interface LeftValue: Node
data class FunctionCall(val id: String,
                        val expressions:List<Expression>,
                        override val position: Position) :LeftValue


data class AssignmentOp(val op:String, override val position:Position):Node
data class AssignmentExpression(val op:AssignmentOp, val expression:Expression, override val position: Position):Node
data class ArithmeticAssignmentOp(val op:String, override val position:Position):Node
data class ArithmeticAssignmentExpression(val op:ArithmeticAssignmentOp, val expression:Expression, override val position: Position):Node

data class ReturnType(val typeSpecifier: TypeSpecifier, override val position: Position):Node

data class FunctionDefinition(val returnType:ReturnType, val id:String, val funcDeclMembers:List<FuncDeclMember>, val statements:List<Statement>, override val position: Position):SimpleStatement

data class FuncDeclMember(val typeSpecifier: TypeSpecifier, val id:String, override val position:Position):Node


data class SelectionStatement(val expression:Expression, val selectionRestStatement: SelectionRestStatement, override val position: Position):SimpleStatement

data class SelectionRestStatement(val statements:List<Statement>, override val position: Position):Node

data class SimpleDeclaration(val qualifier:TypeQualifier?, val specifier:TypeSpecifier, val declarators: List<SimpleDeclarator>, override val position: Position): DeclarationStatement
data class SimpleDeclarator(val leftValue:LeftValue,
                            val arraySpecifiers: List<ArraySpecifier>,
                            val assignmentExpression:AssignmentExpression?,
                            override val position: Position
                            ): Node