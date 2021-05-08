package io.posidon.rpgengine.util

import io.posidon.game.shared.types.Mat4f
import kotlin.math.tan

class ProjectionMatrix(fov: Float, aspectRatio: Float, near: Float, far: Float) : Mat4f() {

    init {
        setFovAndAspectRatio(fov, aspectRatio)
        val range = far - near
        this[2, 2] = -(far + near) / range
        this[2, 3] = -1f
        this[3, 2] = -(2 * far * near) / range
        this[3, 3] = 0f
    }

    fun setFovAndAspectRatio(fov: Float, aspectRatio: Float) {
        val tanFov = tan(Math.toRadians(fov / 2.0)).toFloat()
        this[0, 0] = 1f / (aspectRatio * tanFov)
        this[1, 1] = 1f / tanFov
    }
}