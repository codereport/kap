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
                    job.request.processRequest()
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

    interface Request {
        fun processRequest()
    }

    private inner class EvalAPLRequest(
        val source: SourceLocation,
        val variableBindings: List<Pair<Pair<String, String>, APLValue>>?,
        val callback: (Either<APLValue, Exception>) -> Unit
    ) : Request {
        override fun processRequest() {
            val queueResult = try {
                val resolvedSymbols = variableBindings?.map { (k, v) ->
                    engine.internSymbol(k.second, engine.makeNamespace(k.first)) to v
                }?.toMap()
                val result = engine.parseAndEval(source, extraBindings = resolvedSymbols, allocateThreadLocals = false).collapse()
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

    private inner class ReadVariableRequest(val name: String, val callback: (APLValue?) -> Unit) : Request {
        override fun processRequest() {
            val sym = engine.currentNamespace.findSymbol(name, includePrivate = true)
            val result = if (sym == null) {
                null
            } else {
                TODO("Need to fix")
//                engine.rootContext.environment.findBinding(sym)?.let { binding ->
//                    engine.rootContext.getVar(binding)?.collapse()
//                }
            }
            callback(result)
        }
    }

    private inner class WriteVariableRequest(val name: String, val value: APLValue, val callback: (Exception?) -> Unit) : Request {
        override fun processRequest() {
            TODO("need to fix")
//            val sym = engine.currentNamespace.internSymbol(name)
//            val binding = engine.rootContext.environment.findBinding(sym) ?: engine.rootContext.environment.bindLocal(sym)
//            engine.rootContext.reinitRootBindings()
//            engine.rootContext.setVar(binding, value)
//            callback(null)
        }
    }

    private inner class InternSymbolsRequest(val names: List<Pair<String?, String>>, val callback: (List<Symbol>) -> Unit) : Request {
        override fun processRequest() {
            val symbolList =
                names.map { (namespaceName, symName) -> engine.internSymbol(symName, namespaceName?.let { n -> engine.makeNamespace(n) }) }
            callback(symbolList)
        }
    }

    private fun pushJobToQueue(request: Request): JobId {
        val jobId = JobId()
        queue.add(Job(jobId, request))
        return jobId
    }

    fun pushRequest(
        source: SourceLocation,
        variableBindings: List<Pair<Pair<String, String>, APLValue>>? = null,
        fn: (Either<APLValue, Exception>) -> Unit
    ): JobId {
        return pushJobToQueue(EvalAPLRequest(source, variableBindings, fn))
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
