package io.posidon.rpg.world.tile

import io.posidon.game.shared.types.Vec2i
import kotlinx.serialization.json.*

class TileMeta(
    val width: Int,
    val height: Int,
    val bitmasks: Map<Int, Vec2i>
) {

    companion object {

        fun parse(code: String): TileMeta {
            val json = Json.parseToJsonElement(code).jsonObject
            val width = json["width"]!!.jsonPrimitive.int
            val height = json["height"]!!.jsonPrimitive.int
            val bitmasks = HashMap<Int, Vec2i>()
            json["groups"]!!.jsonArray.forEach {
                val group = it.jsonObject
                when (group["type"]!!.jsonPrimitive.content) {
                    "autotile" -> parseAutotile(group, bitmasks)
                }
            }
            return TileMeta(width, height, bitmasks)
        }

        private fun parseAutotile(
            group: JsonObject,
            bitmasks: HashMap<Int, Vec2i>
        ) {
            group["bitmasks"]!!.jsonArray.forEach {
                val bitmask = it.jsonObject
                val x = bitmask["x"]!!.jsonPrimitive.int
                val y = bitmask["y"]!!.jsonPrimitive.int
                val bits = bitmask["bits"]!!.jsonPrimitive.content.toInt(2)
                bitmasks[bits] = Vec2i(x, y)
            }
        }

        fun serialize(meta: TileMeta) {
            buildJsonObject {
                put("width", meta.width)
                put("height", meta.height)
                put("groups", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "autotile")
                        put("bitmasks", buildJsonArray {
                            meta.bitmasks.forEach {
                                add(buildJsonObject {
                                    put("x", it.value.x)
                                    put("y", it.value.y)
                                    put("bits", it.key.toString(2).padStart(9, '0'))
                                })
                            }
                        })
                    })
                })
            }
        }
    }
}