package net.azisaba.yukielevator.config

import org.bukkit.Material
import org.bukkit.plugin.Plugin

open class ElevatorConfig(plugin: Plugin) : Config(plugin, "config/elevator.yml", "elevator.yml") {

    open var baseBlockType: Material = Material.DIAMOND_BLOCK
        protected set

    open var elevatorHeight: Int = 3
        protected set

    /** エレベーター使用後、上下どちらの操作も受け付けない時間 (ミリ秒) */
    open var cooldownMillis: Long = 500L
        protected set

    override fun onLoad() {
        val typeName = getString("baseBlockType", "DIAMOND_BLOCK")!!
        baseBlockType = Material.matchMaterial(typeName)?.takeIf { it.isBlock } ?: run {
            plugin.logger.warning("baseBlockType '$typeName' は不正なブロックです。DIAMOND_BLOCK を使用します。")
            Material.DIAMOND_BLOCK
        }
        elevatorHeight = getInt("elevatorHeight", 3).coerceAtLeast(1)
        cooldownMillis = getLong("cooldownMillis", 500L).coerceAtLeast(0L)
    }
}
