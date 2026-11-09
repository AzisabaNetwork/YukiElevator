package net.azisaba.yukielevator.elevator

import java.util.UUID

/**
 * 上昇・下降で共有するクールダウン。
 * どちらかの方向でテレポートした直後は、もう一方の方向の操作も受け付けない。
 */
class ElevatorCooldown(
    private val cooldownMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val lastUsed = HashMap<UUID, Long>()

    fun isCoolingDown(uuid: UUID): Boolean {
        val last = lastUsed[uuid] ?: return false
        return clock() - last < cooldownMillis
    }

    fun mark(uuid: UUID) {
        lastUsed[uuid] = clock()
    }

    fun clear(uuid: UUID) {
        lastUsed.remove(uuid)
    }
}
