package array.clientweb2

import kotlinx.browser.document
import kotlinx.html.canvas
import kotlinx.html.dom.create
import kotlinx.html.js.div
import org.w3c.dom.*
import kotlin.js.Promise

data class GuiWindowDescriptor(val id: Int, val toplevel: HTMLDivElement, val canvas: HTMLCanvasElement)

private val windowRegistry = HashMap<Int, GuiWindowDescriptor>()

external fun createImageBitmap(data: dynamic): Promise<ImageBitmap>

fun openWindow(msg: dynamic) {
    println("id=${msg.id}")
    println("w=${msg.width}")
    println("h=${msg.height}")
    val id = msg.id as Int
    println("idObj=${id}")
    val width = msg.width as Int
    val height = msg.height as Int
    println("Created window id = ${id}")
    val toplevelWindow = document.create.div {
        text("This is some text")
        canvas(classes = "gui-frame") {
        }
    }
    val topElement = findElement<HTMLDivElement>("top")
    topElement.appendChild(toplevelWindow)
    val descriptor = GuiWindowDescriptor(id, toplevelWindow, toplevelWindow.childNodes.get(1) as HTMLCanvasElement)
    windowRegistry[id] = descriptor
//    fillWindow(descriptor.canvas)
}

/*
g ← gui:create 1 1
0 ⊣ g gui:draw 200 200 ⍴ 1 0 1 0.5 1 0.2 0.9
 */

fun fillWindow(canvas: HTMLCanvasElement) {
    val context: dynamic = canvas.getContext("2d")
    context.moveTo(0, 0)
    context.lineTo(100, 100)
    context.stroke()
}

fun updateImage(msg: dynamic) {
    val id = msg.id as Int
    val data = msg.data as ImageData
    println("id=${id}, data=${data}")
    val desc = windowRegistry[id]
    if (desc == null) {
        println("Got window update for nonexistent window: ${id}")
        return
    }

//    val context: dynamic = desc.canvas.getContext("bitmaprenderer")
    val canvas: dynamic = desc.canvas
    val w = canvas.width
    val h = canvas.height
    println("w=${w}, h=${h}")
    createImageBitmap(data).then { bitmap ->
        val context = canvas.getContext("2d")
        context.imageSmoothingEnabled = false
//        context.transferFromImageBitmap(bitmap)
        context.drawImage(bitmap, 0, 0, bitmap.width, bitmap.height, 0, 0, canvas.width, canvas.height)
    }
}
