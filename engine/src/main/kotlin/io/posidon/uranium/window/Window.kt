package io.posidon.uranium.window

import io.posidon.uranium.debug.Describable
import io.posidon.uranium.debug.MainLogger
import io.posidon.uranium.debug.i
import io.posidon.uranium.debug.invoke
import io.posidon.uranium.input.InputManager
import io.posidon.uranium.util.Format
import io.posidon.uranium.mathlib.types.Vec2f
import io.posidon.uranium.mathlib.types.Vec2i
import io.posidon.uranium.gfx.renderer.FrameBuffer
import io.posidon.uranium.gfx.renderer.Renderer
import io.posidon.uranium.util.Stack
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30C
import java.util.*

class Window internal constructor(
    private val renderer: Renderer
) : Describable, FrameBuffer {

    inline val size: Vec2i get() = Vec2i(width, height)

    var width: Int
        set(value) {
            _width = value
            GLFW.glfwSetWindowSize(id, width, height)
        }
        get() = _width

    var height: Int
        set(value) {
            _height = value
            GLFW.glfwSetWindowSize(id, width, height)
        }
        get() = _height

    /**
     * The aspect ratio of the display (width:height)
     */
    inline val aspectRatio: Float
        get() = width.toFloat() / height

    /**
     * Sets window title
     */
    var title: String = "game"
        set(value) {
            field = value
            GLFW.glfwSetWindowTitle(id, value)
        }

    /**
     * This is pretty self-explanatory, isn't it?
     */
    inline val shouldClose get() = GLFW.glfwWindowShouldClose(id)

    /**
     * Current DPI / default DPI
     */
    inline val contentScale: Vec2f
        get() = Stack.push {
            val x = it.mallocFloat(1)
            val y = it.mallocFloat(1)
            GLFW.glfwGetWindowContentScale(id, x, y)
            Vec2f(x[0], y[0])
        }

    /**
     * Sets the window to be fullscreen or not
     */
    var isFullscreen = false
        set(value) {
            field = value
            if (value) {
                GLFW.glfwMaximizeWindow(id)
            } else {
                GLFW.glfwRestoreWindow(id)
            }
        }

    var id: Long = 0
        private set

    companion object {
        fun init() {
            //if (GLFW.glfwPlatformSupported(GLFW.GLFW_PLATFORM_WAYLAND))
            //    GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_WAYLAND)
            check(GLFW.glfwInit()) {
                "[GLFW ERROR]: GLFW wasn't inititalized"
            }
            GLFWErrorCallback.createPrint().set()
        }
    }

    private var _width: Int = 0
    private var _height: Int = 0

    internal fun init(
        log: MainLogger,
        width: Int,
        height: Int,
        title: String?,
        callback: InputManager
    ) {
        _width = width
        _height = height
        id = GLFW.glfwCreateWindow(_width, _height, this.title, 0, 0)
        if (title != null) {
            this.title = title
        }
        if (id == 0L) {
            log.e("[GLFW ERROR]: Window wasn't created")
            return
        }
        val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        GLFW.glfwSetWindowPos(id, (videoMode!!.width() - this.width) / 2, (videoMode.height() - this.height) / 2)
        GLFW.glfwSetWindowSizeLimits(id, 600, 300, -1, -1)
        initCallbacks(callback)
        GLFW.glfwShowWindow(id)
        log.verbose {
            i("Created window: ")
            i(this@Window)
        }
    }

    internal inline fun pollEvents() = GLFW.glfwPollEvents()
    internal inline fun swapBuffers() = GLFW.glfwSwapBuffers(id)

    internal fun destroy() {
        destroyCallbacks()
        GLFW.glfwDestroyWindow(id)
        GLFW.glfwTerminate()
    }

    override fun bind() {
        GL30C.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
        GL11C.glViewport(0, 0, width, height)
    }

    override fun describe(): String =
        """Window { 
        |    id: ${Format.pointer(id)}
        |    width: $width
        |    height: $height
        |    title: "${Format.doubleQuotesEscape(title)}"
        |}""".trimMargin()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Window

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()

    private val onResizeListeners = LinkedList<(Window, Int, Int) -> Unit>()

    fun addResizeListener(listener: (Window, width: Int, height: Int) -> Unit) {
        onResizeListeners += listener
    }

    fun removeResizeListener(listener: (Window, Int, Int) -> Unit) {
        onResizeListeners -= listener
    }
    
    private fun initCallbacks(input: InputManager) {
        input.init(this)
        GLFW.glfwSetKeyCallback(id) { _, key, scanCode, action, mods ->
            if (key != GLFW.GLFW_KEY_UNKNOWN) {
                input.onKeyPressed(this, key, scanCode, action, mods)
            }
        }
        GLFW.glfwSetMouseButtonCallback(id) { _, btn, action, mods ->
            input.onMouseButtonPress(this, btn, action, mods)
        }
        GLFW.glfwSetScrollCallback(id) { _, x, y ->
            input.onScroll(this, x, y)
        }
        GLFW.glfwSetCursorPosCallback(id) { _, x, y -> input.onMouseMove(this, x, y) }
        GLFW.glfwSetWindowSizeCallback(id) { _: Long, w: Int, h: Int ->
            _width = w
            _height = h
            renderer.onWindowResize(w, h)
            onResizeListeners.forEach { it(this, w, h) }
        }
    }
    
    private fun destroyCallbacks() {
        GLFW.glfwSetKeyCallback(id, null)?.free()
        GLFW.glfwSetCursorPosCallback(id, null)?.free()
        GLFW.glfwSetMouseButtonCallback(id, null)?.free()
        GLFW.glfwSetScrollCallback(id, null)?.free()
        GLFW.glfwSetWindowSizeCallback(id, null)?.free()
    }
}