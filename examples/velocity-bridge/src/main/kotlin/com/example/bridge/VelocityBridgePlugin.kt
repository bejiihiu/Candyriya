package com.example.bridge

import java.nio.file.Files
import java.nio.file.Path
import kz.bejiihiu.candyriya.plugin.Plugin
import kz.bejiihiu.candyriya.plugin.PluginContext

/**
 * Sketch — how one candyriya plugin can host Velocity plugins.
 *
 * You do NOT need to wait for core to add Velocity support.
 * Implement the bridge as a normal candyriya plugin:
 *
 * 1. add `velocity-api` as `compileOnly` (so proxy jar stays free of it)
 * 2. scan `plugins/VelocityBridge/velocity-plugins/*.jar`
 * 3. instantiate each with Velocity's own `PluginDescription` logic inside your isolated ClassLoader
 * 4. adapt candyriya events/commands to Velocity equivalents via [PluginContext.server] and [PluginContext.events]
 *
 * This file is just a sketch to show the seam. Copy-paste and expand.
 */
public class VelocityBridgePlugin : Plugin {
    private lateinit var ctx: PluginContext
    private var velocityDir: Path? = null

    override fun onEnable(ctx: PluginContext) {
        this.ctx = ctx
        velocityDir = ctx.dataDirectory.resolve("velocity-plugins")
        Files.createDirectories(velocityDir)

        ctx.logger.info("VelocityBridge: scanning {}", velocityDir)
        // pseudo:
        // val jars = Files.list(velocityDir).filter { it.toString().endsWith(".jar") }.toList()
        // for (jar in jars) loadVelocityJar(jar)

        // example: expose a command that lists bridged plugins
        ctx.commands.register(
            "vplugins",
            object : kz.bejiihiu.candyriya.plugin.PluginCommand {
                override val permission: String? = "velocitybridge.list"
                override val description: String = "list bridged Velocity plugins"
                override val usage: String = ""

                override fun execute(
                    source: kz.bejiihiu.candyriya.plugin.PluginCommandSource,
                    args: Array<String>,
                ) {
                    source.sendMessage(
                        net.kyori.adventure.text.Component.text(
                            "Velocity bridge holds N plugins in $velocityDir (not yet loaded in sketch)",
                        ),
                    )
                }
            },
        )

        // adapt candyriya event to Velocity event
        ctx.events.on(ctx.description.id, kz.bejiihiu.candyriya.plugin.PlayerJoinEvent::class.java) { event ->
            // here you would fire com.velocitypowered.api.event.connection.PostLoginEvent
            // via reflection / Velocity EventManager you embedded
            ctx.logger.info("would forward join {} to Velocity event bus", event.player.username)
        }
    }

    // private fun loadVelocityJar(jar: Path) { ... actual Velocity PluginManager code ... }
}

