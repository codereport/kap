package array.gui

import array.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TransferQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class WrappedException(cause: Throwable) : APLGenericException("JVM exception while evaluating expression: ${cause.message}", null, cause)

class Job(val id: JobId, val request: CalculationQueue.Request)
class JobId

class CalculationQueue(val engine: Engine) {
    private val queue: TransferQueue<Job> = LinkedTransferQueue()
    private val thread = Thread { computeLoop() }
    private val taskCompletedHandlers = CopyOnWriteArrayList<(Engine) -> Unit>()
    private var currentJob: Job? = null
    private val lock = ReentrantLock()

    private fun computeLoop() {
        try {
            engine.withThreadLocalAssigned {
                while (!Thread.interrupted()) {
                    val job = queue.take()
                    synchronized(lock) {
                        engine.clearInterrupted()
                        currentJob = job
                    }
                    job.request.processRequest(engine)
                    synchronized(lock) {
                        currentJob = null
                    }
                    fireTaskCompletedHandlers()
                }
            }
        } catch (e: InterruptedException) {
            println("Closing calculation queue")
        }
    }

    fun interruptJob(jobId: JobId) {
        synchronized(lock) {
            val curr = currentJob
            if (curr != null && curr.id === jobId) {
                engine.interruptEvaluation()
            } else {
                queue.removeIf { it.id === jobId }
            }
        }
    }

    fun addTaskCompletedHandler(fn: (Engine) -> Unit) {
        taskCompletedHandlers.add(fn)
    }

    private fun fireTaskCompletedHandlers() {
        taskCompletedHandlers.forEach { fn -> fn(engine) }
    }

    fun interface Request {
        fun processRequest(engine: Engine)
    }

    private inner class EvalAPLRequest(
        val source: SourceLocation,
        val variableBindings: List<Pair<Pair<String, String>, APLValue>>?,
        val preserveNamespace: Boolean = false,
        val inhibitRenderer: Boolean = true,
        val callback: (Either<APLValue, Exception>) -> Unit
    ) : Request {
        override fun processRequest(engine: Engine) {
            val queueResult = try {
                val resolvedSymbols = variableBindings?.map { (k, v) ->
                    engine.internSymbol(k.second, engine.makeNamespace(k.first)) to v
                }?.toMap()

                fun parseSrc() =
                    engine.parseAndEval(source, extraBindings = resolvedSymbols, allocateThreadLocals = false, formatResult = !inhibitRenderer).collapse()
                val result = if (preserveNamespace) {
                    engine.withSavedNamespace {
                        parseSrc()
                    }
                } else {
                    parseSrc()
                }
                Either.Left(result)
            } catch (e: InterruptedException) {
                throw e
            } catch (e: APLGenericException) {
                Either.Right(e)
            } catch (e: Exception) {
                e.printStackTrace()
                Either.Right(WrappedException(e))
            }
            callback(queueResult)
        }
    }

    private inner class ReadVariableRequest(
        val name: String, val callback: (APLValue?) -> Unit) : Request {
        override fun processRequest(engine: Engine) {
            val sym = engine.currentNamespace.findSymbol(name, includePrivate = true)
            val result = if (sym == null) {
                null
            } else {
                engine.rootEnvironment.findBinding(sym)?.let { binding ->
                    val storage = currentStack().findStorage(StackStorageRef(binding))
                    storage.value()
                }
            }
            callback(result)
        }
    }

    private inner class WriteVariableRequest(val name: String, val value: APLValue, val callback: (Exception?) -> Unit) : Request {
        override fun processRequest(engine: Engine) {
            val sym = engine.currentNamespace.internSymbol(name)
            val binding = engine.rootEnvironment.bindLocal(sym)
            engine.recomputeRootFrame()
            val stack = currentStack()
            if (stack.stack.size != 1) {
                throw IllegalStateException("Attempt to write to a variable with active frames")
            }
            val storage = stack.findStorage(StackStorageRef(binding))
            storage.updateValue(value)
            callback(null)
        }
    }

    private inner class InternSymbolsRequest(val names: List<Pair<String?, String>>, val callback: (List<Symbol>) -> Unit) : Request {
        override fun processRequest(engine: Engine) {
            val symbolList =
                names.map { (namespaceName, symName) -> engine.internSymbol(symName, namespaceName?.let { n -> engine.makeNamespace(n) }) }
            callback(symbolList)
        }
    }

    fun pushJobToQueue(request: Request): JobId {
        val jobId = JobId()
        queue.add(Job(jobId, request))
        return jobId
    }

    fun pushRequest(
        source: SourceLocation,
        variableBindings: List<Pair<Pair<String, String>, APLValue>>? = null,
        preserveNamespace: Boolean = false,
        fn: (Either<APLValue, Exception>) -> Unit
    ): JobId {
        return pushJobToQueue(EvalAPLRequest(source, variableBindings, preserveNamespace, false, fn))
    }

    fun pushReadVariableRequest(name: String, callback: (APLValue?) -> Unit): JobId {
        return pushJobToQueue(ReadVariableRequest(name.trim(), callback))
    }

    fun pushWriteVariableRequest(name: String, value: APLValue, fn: (Exception?) -> Unit): JobId {
        return pushJobToQueue(WriteVariableRequest(name.trim(), value, fn))
    }

    fun pushInternSymbolsRequest(names: List<Pair<String?, String>>, fn: (List<Symbol>) -> Unit): JobId {
        return pushJobToQueue(InternSymbolsRequest(names, fn))
    }

    fun start() {
        thread.start()
    }

    fun stop() {
        thread.interrupt()
        thread.join()
        engine.close()
    }

    fun isActive(): Boolean {
        lock.withLock {
            return currentJob != null
        }
    }
}
