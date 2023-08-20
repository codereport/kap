package array.clientweb2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val WINDOW_CREATED_TYPE = "windowCreated"

@Serializable
@SerialName("windowCreated")
class WindowCreated(
    @SerialName("id")
    val id: Long,

    @SerialName("width")
    val width: Int,

    @SerialName("height")
    val height: Int
) : AdditionalOutputData()

const val IMAGE_CONTENT_TYPE = "imageContent"
