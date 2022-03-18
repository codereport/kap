package array.clientweb2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvalRequest(val src: String)

@Serializable
data class PosDescriptor(val line: Int, val col: Int, val name: String?, val callerName: String?)

@Serializable
sealed class ResponseMessage

@Serializable
@SerialName("exception")
data class ExceptionDescriptor(val message: String) : ResponseMessage()

@Serializable
@SerialName("evalexception")
data class EvalExceptionDescriptor(val message: String, val pos: PosDescriptor?) : ResponseMessage()

@Serializable
@SerialName("response")
data class EvalResponse(val result: String) : ResponseMessage()

@Serializable
@SerialName("output")
data class OutputDescriptor(val text: String) : ResponseMessage()

@Serializable
@SerialName("avail")
data class EngineStartedDescriptor(val text: String) : ResponseMessage()
