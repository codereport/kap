package array.sql

import array.*
import java.sql.*

private inline fun <T> withOpenTransaction(conn: Connection, fn: () -> T): T {
    var finished = false
    try {
        val res = fn()
        finished = true
        return res
    } finally {
        if (finished) {
            conn.commit()
        } else {
            conn.rollback()
        }
    }
}

private inline fun <T> withSQLExceptions(pos: Position, fn: () -> T): T {
    return try {
        fn()
    } catch (e: SQLException) {
        throwAPLException(SQLAPLException("Exception from database engine: ${e.message}", pos, e))
    }
}

class SQLAPLException(message: String, pos: Position? = null, cause: Throwable? = null) : APLEvalException(message, pos, cause)

class SQLConnectionValue(val conn: Connection, val description: String) : APLSingleValue() {
    override val aplValueType get() = APLValueType.INTERNAL
    override fun formatted(style: FormatStyle) = "Connection(${description})"
    override fun compareEquals(reference: APLValue): Boolean = reference is SQLConnectionValue && reference.conn == conn
    override fun makeKey(): APLValueKey = APLValueKeyImpl(this, conn)
}

class SQLPreparedStatementValue(val statement: PreparedStatement) : APLSingleValue() {
    override val aplValueType get() = APLValueType.INTERNAL
    override fun formatted(style: FormatStyle) = "PreparedStatement(${statement})"
    override fun compareEquals(reference: APLValue): Boolean = reference is SQLPreparedStatementValue && reference.statement == statement
    override fun makeKey(): APLValueKey = APLValueKeyImpl(this, statement)
}

private fun ensureSQLConnectionValue(a: APLValue, pos: Position? = null): SQLConnectionValue {
    if (a !is SQLConnectionValue) {
        throwAPLException(APLIllegalArgumentException("Value is not a valid SQL connection", pos))
    }
    return a
}

private inline fun <T> withSQLConnection(a: APLValue, pos: Position, fn: (Connection) -> T): T {
    return withSQLExceptions(pos) {
        fn(ensureSQLConnectionValue(a, pos).conn)
    }
}

private inline fun <T> withPreparedStatement(a: APLValue, pos: Position, fn: (PreparedStatement) -> T): T {
    return withSQLExceptions(pos) {
        fn(ensurePreparedStatementValue(a, pos).statement)
    }
}

private fun ensurePreparedStatementValue(a: APLValue, pos: Position? = null): SQLPreparedStatementValue {
    if (a !is SQLPreparedStatementValue) {
        throwAPLException(APLIllegalArgumentException("Value is not a valid prepared statement", pos))
    }
    return a
}

class SQLConnectFunction : APLFunctionDescriptor {
    class SQLConnectFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return withSQLExceptions(pos) {
                val connectionUrl = a.toStringValue(pos)
                val conn = DriverManager.getConnection(connectionUrl)
                conn.autoCommit = false
                SQLConnectionValue(conn, "url=${connectionUrl}")
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SQLConnectFunctionImpl(instantiation)
}

private fun parseEntry(value: Any, colIndex: Int, pos: Position): APLValue {
    return when (value) {
        is Byte -> value.toLong().makeAPLNumber()
        is Short -> value.toLong().makeAPLNumber()
        is Int -> value.toLong().makeAPLNumber()
        is Long -> value.makeAPLNumber()
        is Char -> APLChar(value.code)
        is String -> APLString(value)
        else -> throwAPLException(SQLAPLException("Cannot convert value ${value} to an APL Value (column ${colIndex + 1} in result", pos))
    }
}

private fun resultSetToValue(result: ResultSet, pos: Position): APLValue {
    val metaData = result.metaData
    val colCount = metaData.columnCount
    val resultData = ArrayList<APLValue>()
    while (result.next()) {
        repeat(colCount) { colIndex ->
            resultData.add(parseEntry(result.getObject(colIndex + 1), colIndex, pos))
        }
    }
    return APLArrayList(dimensionsOfSize(resultData.size / colCount, colCount), resultData)
}

class SQLQueryFunction : APLFunctionDescriptor {
    class SQLQueryFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            withSQLConnection(a, pos) {
                val query = b.toStringValue(pos)
                val conn = ensureSQLConnectionValue(a, pos).conn
                return withOpenTransaction(conn) {
                    conn.createStatement().use { statement ->
                        statement.executeQuery(query).use { result ->
                            resultSetToValue(result, pos)
                        }
                    }
                }
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SQLQueryFunctionImpl(instantiation)
}

class SQLUpdateFunction : APLFunctionDescriptor {
    class SQLUpdateFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            withSQLConnection(a, pos) { conn ->
                val query = b.toStringValue(pos)
                return withOpenTransaction(conn) {
                    conn.createStatement().use { statement ->
                        val result = statement.executeUpdate(query)
                        result.makeAPLNumber()
                    }
                }
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SQLUpdateFunctionImpl(instantiation)
}

class SQLPrepareFunction : APLFunctionDescriptor {
    class SQLPrepareFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return withSQLConnection(a, pos) { conn ->
                val statement = conn.prepareStatement(b.toStringValue(pos))
                SQLPreparedStatementValue(statement)
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SQLPrepareFunctionImpl(instantiation)
}

private fun updatePreparedStatementCol(statement: PreparedStatement, index: Int, value: APLValue, pos: Position) {
    when (val v = value.collapse()) {
        is APLLong -> statement.setLong(index, v.value)
        is APLChar -> statement.setString(index, v.asString())
        else -> {
            val stringValue = v.toStringValueOrNull()
            if (stringValue != null) {
                statement.setString(index, stringValue)
            } else {
                throwAPLException(
                    SQLAPLException(
                        "Value cannot be used in an SQL prepared statement: ${value.formatted(FormatStyle.PLAIN)}",
                        pos))
            }
        }
    }
}

class SQLPreparedUpdateFunction : APLFunctionDescriptor {
    class SQLPreparedUpdateFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return withPreparedStatement(a, pos) { statement ->
                withOpenTransaction(statement.connection) {
                    val bDimensions = b.dimensions
                    val multipliers = bDimensions.multipliers()
                    when (bDimensions.size) {
                        1 -> {
                            repeat(bDimensions[0]) { colIndex ->
                                updatePreparedStatementCol(statement, colIndex + 1, b.valueAt(colIndex), pos)
                            }
                            statement.executeUpdate()
                        }
                        2 -> {
                            repeat(bDimensions[0]) { rowIndex ->
                                repeat(bDimensions[1]) { colIndex ->
                                    updatePreparedStatementCol(
                                        statement,
                                        colIndex + 1,
                                        b.valueAt(bDimensions.indexFromPosition(intArrayOf(rowIndex, colIndex), multipliers, pos)),
                                        pos)
                                }
                                statement.executeUpdate()
                            }
                        }
                        else -> throwAPLException(SQLAPLException("Right value must be rank 1 or 2", pos))
                    }
                }
                APLNullValue.APL_NULL_INSTANCE
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SQLPreparedUpdateFunctionImpl(instantiation)
}

class SQLPreparedQueryFunction : APLFunctionDescriptor {
    class SQLPreparedQueryFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            withPreparedStatement(a, pos) { statement ->
                withOpenTransaction(statement.connection) {
                    val bCollapsed = b.collapse()
                    val bDimensions = bCollapsed.dimensions
                    if (bDimensions.size != 1) {
                        throwAPLException(SQLAPLException("Right argument to function must be a rank-1 array", pos))
                    }
                    repeat(bDimensions[0]) { colIndex ->
                        updatePreparedStatementCol(statement, colIndex + 1, bCollapsed.valueAt(colIndex), pos)
                    }
                    statement.executeQuery().use { result ->
                        return resultSetToValue(result, pos)
                    }
                }
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SQLPreparedQueryFunctionImpl(instantiation)
}

class SQLModule : KapModule {
    override val name get() = "sql"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("sql")
        engine.registerFunction(ns.internAndExport("connect"), SQLConnectFunction())
        engine.registerFunction(ns.internAndExport("query"), SQLQueryFunction())
        engine.registerFunction(ns.internAndExport("update"), SQLUpdateFunction())
        engine.registerFunction(ns.internAndExport("prepare"), SQLPrepareFunction())
        engine.registerFunction(ns.internAndExport("updatePrepared"), SQLPreparedUpdateFunction())
        engine.registerFunction(ns.internAndExport("queryPrepared"), SQLPreparedQueryFunction())

        engine.registerClosableHandler(object : ClosableHandler<SQLConnectionValue> {
            override fun close(value: SQLConnectionValue) {
                value.conn.close()
            }
        })
        engine.registerClosableHandler(object : ClosableHandler<SQLPreparedStatementValue> {
            override fun close(value: SQLPreparedStatementValue) {
                value.statement.close()
            }
        })
    }
}
