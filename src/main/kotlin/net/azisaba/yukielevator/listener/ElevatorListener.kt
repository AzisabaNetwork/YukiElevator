package net.azisaba.yukielevator.listener

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import net.azisaba.yukielevator.YukiElevator
import net.azisaba.yukielevator.elevator.ElevatorCooldown
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerToggleSneakEvent

class ElevatorListener(
    private val plugin: YukiElevator,
    private val cooldown: ElevatorCooldown,
) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onElevatorUp(event: PlayerJumpEvent) {
        // PlayerJumpEvent はキー入力ではなく移動パケットから推測して発火するため、
        // テレポート直後などに誤発火することがある。
        // そのため「直前の位置」が床ブロックの天面上であることを必ず確認する。
        tryMove(event.player, event.from, BlockFace.UP)
    }

    @EventHandler(ignoreCancelled = true)
    fun onElevatorDown(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) {
            return
        }
        tryMove(event.player, event.player.location, BlockFace.DOWN)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cooldown.clear(event.player.uniqueId)
    }

    private fun tryMove(player: Player, playerFrom: Location, face: BlockFace) {
        if (player.isDead || player.isFlying || player.isSwimming || player.isInsideVehicle) {
            return
        }
        if (cooldown.isCoolingDown(player.uniqueId)) {
            return
        }

        val system = plugin.system
        val baseFrom = playerFrom.block.getRelative(BlockFace.DOWN)
        if (!system.isFloor(baseFrom) || !system.isStandingOn(playerFrom, baseFrom)) {
            return
        }

        val baseTo = system.tryFindFloor(baseFrom, face) ?: return

        val permission = if (face == BlockFace.UP) "yukielevator.up" else "yukielevator.down"
        if (!player.hasPermission(permission)) {
            val action = if (face == BlockFace.UP) "上る" else "下る"
            player.sendMessage(Component.text("あなたはエレベーターを${action}権限を持っていません！", NamedTextColor.RED))
            return
        }

        val playerTo = system.calculatePlayerTo(playerFrom, baseFrom, baseTo)
        if (!player.teleport(playerTo, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            return
        }
        cooldown.mark(player.uniqueId)
        player.fallDistance = 0f

        player.playSound(playerTo, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
        player.world.spawnParticle(Particle.TOTEM_OF_UNDYING, playerTo, 50, 0.2, 0.2, 0.2, 0.5)
    }
}
