package org.openrndr.draw

/**
 * A token of GLSL code: an identifier, a number, a preprocessor directive, a comment, white space or a symbol
 */
internal class GlslToken(val text: String, val kind: Kind, val line: Int) {
    enum class Kind { IDENTIFIER, NUMBER, DIRECTIVE, COMMENT, SPACE, SYMBOL }
}

/**
 * Splits GLSL code in tokens, joining the tokens gives the code back
 */
internal fun glslTokens(code: String): List<GlslToken> {
    val tokens = mutableListOf<GlslToken>()
    var i = 0
    var line = 0
    var lineStart = true
    while (i < code.length) {
        val c = code[i]
        val start = i
        val kind = when {
            c == '#' && lineStart -> {
                // a directive runs to the end of the line, the identifiers in it are tokens of their own
                i++
                GlslToken.Kind.DIRECTIVE
            }

            code.startsWith("//", i) -> {
                while (i < code.length && code[i] != '\n') i++
                GlslToken.Kind.COMMENT
            }

            code.startsWith("/*", i) -> {
                val end = code.indexOf("*/", i + 2)
                i = if (end == -1) code.length else end + 2
                GlslToken.Kind.COMMENT
            }

            c.isLetter() || c == '_' -> {
                while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                GlslToken.Kind.IDENTIFIER
            }

            c.isDigit() || (c == '.' && i + 1 < code.length && code[i + 1].isDigit()) -> {
                while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '.' || code[i] == '_' ||
                            ((code[i] == '+' || code[i] == '-') && (code[i - 1] == 'e' || code[i - 1] == 'E')))
                ) i++
                GlslToken.Kind.NUMBER
            }

            c.isWhitespace() -> {
                while (i < code.length && code[i].isWhitespace()) i++
                GlslToken.Kind.SPACE
            }

            else -> {
                i++
                GlslToken.Kind.SYMBOL
            }
        }
        val text = code.substring(start, i)
        tokens.add(GlslToken(text, kind, line))
        line += text.count { it == '\n' }
        if (kind == GlslToken.Kind.SPACE) {
            if ('\n' in text) lineStart = true
        } else if (kind != GlslToken.Kind.COMMENT) {
            lineStart = false
        }
    }
    return tokens
}

private val glslNonNames = setOf(
    "main", "precision", "struct", "layout", "uniform", "in", "out", "inout", "const", "flat", "smooth",
    "noperspective", "centroid", "invariant", "highp", "mediump", "lowp", "buffer", "shared", "readonly",
    "writeonly", "coherent", "volatile", "restrict"
)

/**
 * The names that are declared at the top level of [code]: functions, global variables, varyings, structs and macros
 */
internal fun glslTopLevelNames(code: String): Set<String> {
    val names = mutableSetOf<String>()
    val tokens = glslTokens(code).filter { it.kind != GlslToken.Kind.SPACE && it.kind != GlslToken.Kind.COMMENT }

    // the tokens of the statement at the top level, with nested parentheses and brackets as single entries
    var statement = mutableListOf<String>()
    var depth = 0
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        if (token.kind == GlslToken.Kind.DIRECTIVE) {
            // #define NAME or #define NAME(arguments)
            if (tokens.getOrNull(i + 1)?.text == "define") {
                tokens.getOrNull(i + 2)?.takeIf { it.kind == GlslToken.Kind.IDENTIFIER }?.let { names.add(it.text) }
            }
            // skip the rest of the directive, tokens on the same line belong to it
            val line = token.line
            i++
            while (i < tokens.size && tokens[i].line == line) i++
            continue
        }
        when (token.text) {
            "(", "[" -> {
                if (depth == 0) statement.add(token.text)
                depth++
            }

            ")", "]" -> {
                depth--
                if (depth == 0) statement.add(token.text)
            }

            "{" -> {
                if (depth == 0) {
                    // a function definition or a struct: the name is before the parameters or after `struct`
                    val structIndex = statement.indexOf("struct")
                    if (structIndex >= 0) {
                        statement.getOrNull(structIndex + 1)?.let { names.add(it) }
                    } else {
                        functionName(statement)?.let { names.add(it) }
                    }
                    // skip the body
                    var bodyDepth = 1
                    i++
                    while (i < tokens.size && bodyDepth > 0) {
                        if (tokens[i].text == "{") bodyDepth++
                        if (tokens[i].text == "}") bodyDepth--
                        i++
                    }
                    // a struct may declare variables after its body, a function ends here
                    if (structIndex >= 0) {
                        statement = mutableListOf("struct_body")
                    } else {
                        statement = mutableListOf()
                    }
                    continue
                }
                depth++
            }

            "}" -> depth--
            ";" -> {
                if (depth == 0) {
                    names.addAll(declaredNames(statement))
                    statement = mutableListOf()
                }
            }

            else -> if (depth == 0) statement.add(token.text)
        }
        i++
    }
    return names - glslNonNames
}

/** the name of a function prototype or definition `type name(...)` */
private fun functionName(statement: List<String>): String? {
    val open = statement.indexOf("(")
    if (open < 1 || statement.lastOrNull() != ")") return null
    return statement[open - 1].takeIf { it.first().isLetter() || it.first() == '_' }
}

/** the names declared by a statement: a function prototype or variables, `type a, b = 1.0, c[2]` */
private fun declaredNames(statement: List<String>): List<String> {
    if (statement.isEmpty() || statement.first() == "precision") return emptyList()
    functionName(statement)?.let { return listOf(it) }
    val names = mutableListOf<String>()
    var declarator = mutableListOf<String>()
    fun finish() {
        // the name is the last identifier before an initializer or array size
        val end = declarator.indexOfFirst { it == "=" || it == "[" }.let { if (it == -1) declarator.size else it }
        declarator.subList(0, end).lastOrNull { it.first().isLetter() || it.first() == '_' }?.let { names.add(it) }
        declarator = mutableListOf()
    }
    for (token in statement) {
        if (token == ",") finish() else declarator.add(token)
    }
    finish()
    return names
}

/**
 * Renames the identifiers of [code] that are keys of [renames], identifiers that are fields or swizzles (`v.x`) are
 * not renamed
 */
internal fun glslRename(code: String, renames: Map<String, String>): String {
    if (renames.isEmpty()) return code
    val tokens = glslTokens(code)
    val result = StringBuilder()
    var previous: GlslToken? = null
    for (token in tokens) {
        val renamed = renames[token.text]
        if (token.kind == GlslToken.Kind.IDENTIFIER && renamed != null && previous?.text != ".") {
            result.append(renamed)
        } else {
            result.append(token.text)
        }
        if (token.kind != GlslToken.Kind.SPACE && token.kind != GlslToken.Kind.COMMENT) previous = token
    }
    return result.toString()
}
