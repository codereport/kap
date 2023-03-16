package array.gui.reporting.edit

import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.io.File


/**
 * A custom object which contains a file path to an image file.
 * When rendered in the rich text editor, the image is loaded from the
 * specified file.
 */
class RealLinkedImage(imagePath: String) : LinkedImage {
    override val imagePath: String

    /**
     * Creates a new linked image object.
     *
     * @param imagePath The path to the image file.
     */
    init {

        // if the image is below the current working directory,
        // then store as relative path name.
        var imagePath0 = imagePath
        val currentDir = System.getProperty("user.dir") + File.separatorChar
        if (imagePath0.startsWith(currentDir)) {
            imagePath0 = imagePath0.substring(currentDir.length)
        }
        this.imagePath = imagePath0
    }

    override val isReal: Boolean
        get() = true

    override fun toString(): String {
        return String.format("RealLinkedImage[path=%s]", imagePath)
    }

    override fun createNode(): Node? {
        val image =
            Image("file:$imagePath") // XXX: No need to create new Image objects each time -
        // could be cached in the model layer
        return ImageView(image)
    }
}
