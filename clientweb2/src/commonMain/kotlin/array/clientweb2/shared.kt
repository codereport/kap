package array.clientweb2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvalRequest(val src: String)

@Serializable
sealed abstract class ResponseMessage

@Serializable
@SerialName("error")
data class ExceptionDescriptor(val message: String) : ResponseMessage()

@Serializable
@SerialName("response")
data class EvalResponse(val result: String) : ResponseMessage()

@Serializable
@SerialName("output")
data class Output(val text: String) : ResponseMessage()
