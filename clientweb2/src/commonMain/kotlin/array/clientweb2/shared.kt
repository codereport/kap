package array.clientweb2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

///////////////////////////////////////////////
// Worker communication structures
///////////////////////////////////////////////


@Serializable
enum class ResultType {
    FORMATTED_PRETTY,
    FORMATTED_READABLE,
    JS
}

@Serializable
data class EvalRequest(val src: String, val resultType: ResultType)

@Serializable
data class PosDescriptor(val line: Int, val col: Int, val callerName: String?)

@Serializable
sealed class ResponseMessage

@Serializable
@SerialName("exception")
data class ExceptionDescriptor(val message: String) : ResponseMessage()

@Serializable
@SerialName("evalexception")
data class EvalExceptionDescriptor(val message: String, val pos: PosDescriptor?) : ResponseMessage()

@Serializable
sealed class EvalResponse : ResponseMessage()

@Serializable
@SerialName("stringresponse")
data class StringResponse(val result: String) : EvalResponse()

@Serializable
@SerialName("dataresponse")
data class DataResponse(val result: JsKapValue) : EvalResponse()

@Serializable
@SerialName("output")
data class OutputDescriptor(val text: String) : ResponseMessage()

@Serializable
@SerialName("avail")
data class EngineStartedDescriptor(val text: String) : ResponseMessage()

///////////////////////////////////////////////
// JSON representation of KAP datatypes
///////////////////////////////////////////////

@Serializable
sealed class JsKapValue

@Serializable
@SerialName("string")
data class JsKapString(val value: String) : JsKapValue()

@Serializable
sealed class JsKapNumber : JsKapValue()

@Serializable
@SerialName("integer")
data class JsKapInteger(val value: String) : JsKapNumber()

@Serializable
@SerialName("double")
data class JsKapDouble(val value: Double) : JsKapNumber()

@Serializable
@SerialName("char")
data class JsKapChar(val value: String) : JsKapValue()

@Serializable
@SerialName("array")
data class JsKapArray(val dimensions: List<Int>, val values: List<JsKapValue>) : JsKapValue()

@Serializable
@SerialName("list")
data class JsKapList(val values: List<JsKapValue>) : JsKapValue()

@Serializable
@SerialName("undefined")
data class JsKapUndefined(val value: String) : JsKapValue()
