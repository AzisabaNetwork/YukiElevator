package net.azisaba.yukielevator.config

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level

open class Config(
    val plugin: Plugin,
    val resourceSupplier: () -> InputStream?,
    val outputFile: Path,
) : YamlConfiguration() {

    constructor(plugin: Plugin, resourcePath: String, outputPath: String) : this(
        plugin,
        { plugin.javaClass.classLoader.getResourceAsStream(resourcePath) },
        plugin.dataFolder.toPath().resolve(outputPath),
    )

    fun saveDefaultConfig() {
        try {
            if (!Files.isRegularFile(outputFile)) {
                Files.createDirectories(outputFile.parent)
                resourceSupplier()?.use { Files.copy(it, outputFile) }
            }
        } catch (ex: IOException) {
            plugin.logger.log(Level.SEVERE, "設定ファイルの保存中にエラーが発生しました。", ex)
        }
    }

    fun saveDefaultConfigAsync() {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { saveDefaultConfig() })
    }

    open fun onLoad() {}

    open fun onSave() {}

    fun loadConfig() {
        try {
            if (Files.isRegularFile(outputFile)) {
                load(outputFile.toFile())
            } else {
                resourceSupplier()?.use { load(InputStreamReader(it, StandardCharsets.UTF_8)) }
                saveDefaultConfigAsync()
            }
        } catch (ex: IOException) {
            plugin.logger.log(Level.SEVERE, "設定ファイルの読み込み中にエラーが発生しました。", ex)
        } catch (ex: InvalidConfigurationException) {
            plugin.logger.log(Level.SEVERE, "設定ファイルの書式が間違っています。", ex)
        }
        onLoad()
    }

    fun saveConfig() {
        onSave()
        try {
            save(outputFile.toFile())
        } catch (ex: IOException) {
            plugin.logger.log(Level.SEVERE, "設定ファイルの保存中にエラーが発生しました。", ex)
        }
    }
}
