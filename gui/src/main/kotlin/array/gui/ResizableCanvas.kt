package array.gui

import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.scene.canvas.Canvas
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


class ResizableCanvas : Canvas() {
    override fun isResizable() = true

    private val changeListenerTimer = Timer()
    private var changeListenerTask: TimerTask? = null
    private val changeListeners = CopyOnWriteArrayList<() -> Unit>()

    init {
        val listener = ChangeListener<Number> { observable, oldValue, newValue ->
            changeListenerTask?.cancel()
            changeListenerTask = object : TimerTask() {
                override fun run() {
                    Platform.runLater {
                        changeListeners.forEach { v -> v() }
                    }
                }
            }
            changeListenerTimer.schedule(changeListenerTask, 100)
        }
        widthProperty().addListener(listener)
        heightProperty().addListener(listener)
    }

    override fun maxHeight(width: Double): Double {
        return Double.POSITIVE_INFINITY
    }

    override fun maxWidth(height: Double): Double {
        return Double.POSITIVE_INFINITY
    }

    override fun minWidth(height: Double): Double {
        return 1.0
    }

    override fun minHeight(width: Double): Double {
        return 1.0
    }

    override fun resize(width: Double, height: Double) {
        this.setWidth(width)
        this.setHeight(height)
    }

    fun addOnSizeChangeCallback(fn: () -> Unit) {
        changeListeners.add(fn)
    }
}
