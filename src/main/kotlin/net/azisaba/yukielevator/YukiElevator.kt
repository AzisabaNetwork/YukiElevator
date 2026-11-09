package net.azisaba.yukielevator

import net.azisaba.yukielevator.config.ElevatorConfig
import net.azisaba.yukielevator.elevator.ElevatorCooldown
import net.azisaba.yukielevator.elevator.ElevatorSystem
import net.azisaba.yukielevator.listener.ElevatorListener
import org.bukkit.plugin.java.JavaPlugin

class YukiElevator : JavaPlugin() {

    private lateinit var elevatorConfig: ElevatorConfig

    lateinit var system: ElevatorSystem
        private set

    override fun getConfig(): ElevatorConfig = elevatorConfig

    override fun onEnable() {
        elevatorConfig = ElevatorConfig(this).apply { loadConfig() }
        system = ElevatorSystem(elevatorConfig)

        val cooldown = ElevatorCooldown(elevatorConfig.cooldownMillis)
        server.pluginManager.registerEvents(ElevatorListener(this, cooldown), this)
    }
}
