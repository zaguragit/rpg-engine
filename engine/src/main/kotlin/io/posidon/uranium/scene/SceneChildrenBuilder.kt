package io.posidon.uranium.scene

import io.posidon.uranium.mathlib.types.Vec2f
import io.posidon.uranium.mathlib.types.Vec2i
import io.posidon.uranium.gfx.assets.Texture
import io.posidon.uranium.gfx.assets.Uniforms
import io.posidon.uranium.gfx.assets.invoke
import io.posidon.uranium.gfx.renderer.Renderer
import io.posidon.uranium.gfx.renderer.UIRenderer
import io.posidon.uranium.mathlib.types.Vec3f
import io.posidon.uranium.scene.node.Node
import io.posidon.uranium.tools.Filter
import io.posidon.uranium.tools.Camera2D
import io.posidon.uranium.tools.Camera3D
import io.posidon.uranium.window.Window
import java.util.LinkedList
import kotlin.math.min

class SceneChildrenBuilder(
    val renderer: Renderer,
    private val window: Window
) {

    internal val layers = LinkedList<LayerBuilder>()

    fun customLayer(renderer: Renderer, init: LayerBuilder.() -> Unit) {
        layers += LayerBuilder(renderer, window).apply(init)
    }

    fun camera2DLayer(cameraFollowPos: Vec2f, init: LayerBuilderWithCamera2D.() -> Unit) {
        layers += LayerBuilderWithCamera2D(Camera2D(renderer, cameraFollowPos), window).apply(init)
    }

    fun camera3DLayer(init: LayerBuilderWithCamera3D.() -> Unit) {
        layers += LayerBuilderWithCamera3D(Camera3D(renderer, Vec3f.zero(), Vec3f.zero(), 70f), window).apply(init)
    }

    fun uiLayer(init: LayerBuilder.() -> Unit) = customLayer(UIRenderer(renderer), init)

    class LayerBuilderWithCamera2D internal constructor(
        val camera: Camera2D,
        window: Window
    ) : LayerBuilder(camera.renderer, window)

    class LayerBuilderWithCamera3D internal constructor(
        val camera: Camera3D,
        window: Window
    ) : LayerBuilder(camera.renderer, window)

    class FilterBuilder internal constructor(
        val minWidth: Int
    ) {
        internal val nodes = LinkedList<Node>()

        operator fun Node.unaryMinus() {
            nodes += this
        }

        internal var uniforms: Uniforms.() -> Unit = {}
            private set

        fun shader(block: Uniforms.() -> Unit) {
            uniforms = block
        }

        fun resolution(window: Window): Vec2i = Filter.calculateBufferSize(window.width, window.height, minWidth)
    }

    open class LayerBuilder internal constructor(
        internal val renderer: Renderer,
        private val window: Window
    ) {

        internal val nodes = LinkedList<Node>()

        operator fun Node.unaryMinus() {
            nodes += this
        }

        fun post(fragmentPath: String, colorBufferCount: Int, minWidth: Int = min(window.width, window.height), block: FilterBuilder.() -> Unit): Filter {
            val builder = FilterBuilder(minWidth).apply(block)
            return Filter(renderer, window, fragmentPath, colorBufferCount, minWidth, builder.uniforms, builder.nodes)
        }

        fun background(fragmentPath: String, texture: Texture? = null, uniforms: Uniforms.() -> Unit): Node {
            return object : Node() {
                val shader by screenShader(fragmentPath)
                override fun render(renderer: Renderer, window: Window) {
                    texture?.bind(0)
                    shader(uniforms)
                    renderer.renderScreen(window, shader)
                }
            }
        }
    }
}