package org.openrndr.shadestyle


import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import java.util.*

class ShadeStyleError(val message: String, val line: Int, val position: Int)

fun verifyShadeStyle(code: String):Prog {
    val errors = mutableListOf<ShadeStyleError>()
    val iis = ANTLRInputStream(code)
    val lexer = ShadeStyleLexer(iis)
    lexer.removeErrorListeners()
    val listener = object : ANTLRErrorListener {
        override fun reportAttemptingFullContext(parser: Parser?, dfa: DFA?, startIndex: Int, stopIndex: Int, conflictingAlts: BitSet?, configs: ATNConfigSet?) {
            //errors.add(ShadeStyleError("attempting full context", -1, -1))
        }

        override fun syntaxError(p0: Recognizer<*, *>?, p1: Any?, p2: Int, p3: Int, p4: String?, p5: RecognitionException?) {
            errors.add(ShadeStyleError(p4 ?: "", p2, p3))
        }

        override fun reportAmbiguity(p0: Parser?, p1: DFA?, p2: Int, p3: Int, p4: Boolean, p5: BitSet?, p6: ATNConfigSet?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun reportContextSensitivity(parser: Parser?, dfa: DFA?, startIndex: Int, stopIndex: Int, prediction: Int, configs: ATNConfigSet?) {
            //TODO("not implemented") //To chan ge body of created functions use File | Settings | File Templates.
        }

    }
    lexer.addErrorListener(listener)
    val tokens = CommonTokenStream(lexer)
    val parser = ShadeStyleParser(tokens)
    parser.removeErrorListeners()
    parser.addErrorListener(listener)
    val tree = parser.prog()

    errors.forEach {
        println(it.message)
    }

    val ast = ShadeStyleParseTreeToAstMapper().map(tree)
    return ast
}

fun generate(node: Node, indent:Int=0): String {

    return when (node) {

        is Paren -> "(${generate(node.expression)})"
        is ArraySpecifier -> "[${generate(node.expression)}]"
        is SimpleDeclarator -> "${generate(node.leftValue)}${node.arraySpecifiers.joinToString { generate(it) }.spaceLeft()}${(node.assignmentExpression?.let { generate(it) }?:"").spaceLeft()}"
        is ReturnType -> generate(node.typeSpecifier)
        is StructSpecifier -> ".${generate(node.expression)}"

        is ForInitStatement -> "${node.basicStatements.joinToString(",") { generate(it) }};"
        is ForCondStatement -> "${generate(node.expression)};"
        is ForRestStatement -> "${node.basicStatements.joinToString(",") { generate(it) }}"

        is AssignmentExpression -> "${node.op.op} ${generate(node.expression)}"
        is SelectionRestStatement -> "${node.statements.joinToString("\n"){ generate(it, indent)}}"
        is ConstantExpression -> when(node) {
            is IntegerConstant -> "${node.value}"
            is FloatConstant -> "${node.value}"
            else -> TODO(node.javaClass.canonicalName)
        }

        is BasicType -> when(node) {
            is ScalaType -> node.type
            is VectorType -> node.type
            is VoidType -> "void"
            else -> TODO(node.javaClass.canonicalName)
        }
        is FuncDeclMember -> return "${generate(node.typeSpecifier)} ${node.id}"

        is TypeQualifier -> return node.qualifier
        is TypeSpecifier -> {
            "${generate(node.typeSpecifierNonArray)}"

        }
        is TypeSpecifierNonArray -> {
            when (node) {
                is Identifier -> node.id
                else -> TODO(node.javaClass.canonicalName)

            }
        }
        is Prog -> node.statements.joinToString("\n") { generate(it) }
        is Expression -> when(node) {
            is CompareOpExpression -> "${generate(node.expressions[0])} ${node.op.op} ${generate(node.expressions[1])}"
            is IncrementOpExpresssion -> "${node.op.op}${generate(node.expressions)}"
            is AddDivExpression -> "${generate(node.expressions[0])} ${node.op.op} ${generate(node.expressions[1])}"
            is MulDivExpression -> "${generate(node.expressions[0])} ${node.op.op} ${generate(node.expressions[1])}"
            is EqualOpExpression -> "${generate(node.expressions[0])} ${node.op.op} ${generate(node.expressions[1])}"
            is PrimaryExpression -> when(node) {
                is VectorExpression -> "${generate(node.basicType)}(${node.expressions.joinToString(",") { generate(it) }})"
                is LeftValueIndex -> "${generate(node.leftValue)}${node.arrayStructSelection?.let{generate(it)}?:""}"
                is ArrayStructSelection -> listOf(node.arraySpecifiers.joinToString(" ") { generate(it) }, node.structSpecifiers.joinToString(" ") { generate(it) }).joinToString("")
                else -> TODO(node.javaClass.canonicalName)
            }
            else -> TODO(node.javaClass.canonicalName)

        }
        is Statement -> when (node) {

            is SimpleStatement -> when (node) {
                is ForStatement -> "for(${generate(node.initStatement)}${generate(node.condStatement)}${generate(node.restStatment)}) {\n ${node.statements.joinToString("\n") { generate(it, indent+4) }}" +
                        "\n}".leftPad(indent)

                is FunctionDefinition ->
                    """${generate(node.returnType)} ${node.id}(${node.funcDeclMembers.joinToString(",") { generate(it) }} ) {
                          |${node.statements.joinToString("\n") { generate(it, indent+4) }}
                        |}
                    """.trimMargin().leftPad(indent)
                is SelectionStatement ->
                    "if (${generate(node.expression)}) ${generate(node.selectionRestStatement, indent+4)}".leftPad(indent)
                is BasicStatementWithSemicolon -> "${generate(node.basicStatement)};".leftPad(indent)

                is BasicStatement -> when (node) {
                    is SimpleDeclaration -> "${(node.qualifier?.let { generate(it) }?:"").space()}${generate(node.specifier)}${node.declarators.joinToString(",") { generate(it) }.spaceLeft()}".leftPad(indent)
                    is AssignmentStatement -> when (node) {
                        is NormalAssignmentStatement -> "${generate(node.leftValue)}${(node.arrayStructSelection?.let { generate(it) }?:"")} = ${generate(node.assignmentExpression.expression)}".leftPad(indent)
                        is ArithmeticAssignmentStatement -> "${generate(node.leftValue)} ${generate(node.assignmentExpression.expression)}"
                        else -> TODO(node.javaClass.canonicalName)
                    }

                    is ExpressionStatement -> "${generate(node.expression)}".leftPad(indent)
                    else -> TODO(node.javaClass.canonicalName)
                }
                is JumpStatement -> when(node) {
                    is ReturnStatement -> "return ${node.expression?.let { generate(it) }?:""};".leftPad(indent)
                    else -> TODO(node.javaClass.canonicalName)
                }
                else -> TODO(node.javaClass.canonicalName)

            }
            is CompoundStatement -> "{\n${node.statements.joinToString("\n") { generate(it, indent) }}\n${"}".leftPad(indent-4)}"
            else -> TODO(node.javaClass.canonicalName)

        }
        else -> TODO(node.javaClass.canonicalName)
    }
}

private fun String.leftPad(i: Int):String {

    val pad = (0 until i).joinToString("") { " " }
    return "$pad$this"
}

private fun String.space(): String {
    if (this.isNotEmpty()) {
        return "$this "
    } else {
        return this
    }
}

private fun String.spaceLeft(): String {
    if (this.isNotEmpty()) {
        return " $this"
    } else {
        return this
    }
}


fun isOut(node:Node):Boolean {
    if (node is BasicStatementWithSemicolon) {
        if (node.basicStatement is SimpleDeclaration) {
            if (node.basicStatement.qualifier?.qualifier == "out") {
                return true
            }
        }
    }
    return false
}

fun main(args: Array<String>) {


    val ast = verifyShadeStyle("""
        void doStuff() {
            float k = 5;
        }
        out vec3 bla;
        m[m[3][3]][2] = 1.0;
        if (x < 0) {
            float a = 4.0;
            if (x < 0) {
                float a = 4.0;
            }
        }
        """)


    val outs = ast.copy(statements = ast.statements.filter { isOut(it) })
    val functions = ast.copy(statements = ast.statements.filter { it is FunctionDefinition  })
    val otherCode = ast.copy(statements = ast.statements.filter { it !is FunctionDefinition && !isOut(it) })

    println(generate(outs))
    println("---")

    println(generate(functions))
    println("---")
    println(generate(otherCode))

}