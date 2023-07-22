package array.gui.reporting.edit

import javafx.scene.Node
import org.fxmisc.richtext.model.Codec
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException


interface LinkedImage {
    val isReal: Boolean

    /**
     * @return The Paths.get the image to render.
     */
    val imagePath: String

    fun createNode(): Node

    companion object {
        fun codec(): Codec<LinkedImage> {
            return object : Codec<LinkedImage> {
                override fun getName(): String {
                    return "LinkedImage"
                }

                @Throws(IOException::class)
                override fun encode(os: DataOutputStream, linkedImage: LinkedImage) {
                    if (linkedImage.isReal) {
                        os.writeBoolean(true)
                        val externalPath = linkedImage.imagePath.replace("\\", "/")
                        Codec.STRING_CODEC.encode(os, externalPath)
                    } else {
                        os.writeBoolean(false)
                    }
                }

                @Throws(IOException::class)
                override fun decode(`is`: DataInputStream): LinkedImage {
                    return if (`is`.readBoolean()) {
                        var imagePath = Codec.STRING_CODEC.decode(`is`)
                        imagePath = imagePath.replace("\\", "/")
                        RealLinkedImage(imagePath)
                    } else {
                        EmptyLinkedImage()
                    }
                }
            }
        }
    }
}
