package net.azisaba.yukielevator.elevator

import net.azisaba.yukielevator.config.ElevatorConfig
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace

class ElevatorSystem(config: ElevatorConfig) {

    val baseType: Material = config.baseBlockType
    val height: Int = config.elevatorHeight

    fun isSafe(block: Block?): Boolean {
        if (block == null || !block.isPassable) {
            return false
        }
        return block.type !in UNSAFE_TYPES
    }

    fun isFloor(baseFrom: Block?): Boolean {
        if (baseFrom == null || baseFrom.type != baseType) {
            return false
        }
        return (1 until height).all { isSafe(baseFrom.getRelative(BlockFace.UP, it)) }
    }

    fun tryFindFloor(baseFrom: Block?, face: BlockFace): Block? {
        if (baseFrom == null || (face != BlockFace.UP && face != BlockFace.DOWN)) {
            return null
        }

        val world = baseFrom.world
        val step = if (face == BlockFace.UP) 1 else -1
        val minY = world.minHeight
        val maxY = world.maxHeight

        var y = baseFrom.y + step * height
        while (y in minY until maxY) {
            val target = world.getBlockAt(baseFrom.x, y, baseFrom.z)
            if (isFloor(target)) {
                return target
            }
            if (!isSafe(target)) {
                return null
            }
            y += step
        }
        return null
    }

    /**
     * プレイヤーがエレベーターの床 (base) の上に立っているかを判定する。
     * 足元の Y が base の天面とほぼ一致している必要がある。
     */
    fun isStandingOn(playerLocation: Location, base: Block): Boolean {
        if (playerLocation.world != base.world) {
            return false
        }
        if (playerLocation.blockX != base.x || playerLocation.blockZ != base.z) {
            return false
        }
        return kotlin.math.abs(playerLocation.y - (base.y + 1)) < STANDING_EPSILON
    }

    fun calculatePlayerTo(playerFrom: Location, baseFrom: Block, baseTo: Block): Location {
        val playerTo = baseTo.getRelative(BlockFace.UP).location

        val xzDiff = playerFrom.clone().subtract(baseFrom.location)
        xzDiff.y = 0.0
        playerTo.add(xzDiff)

        playerTo.direction = playerFrom.direction
        return playerTo
    }

    companion object {
        private const val STANDING_EPSILON = 0.1

        private val UNSAFE_TYPES = setOf(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
        )
    }
}
