package com.example

import java.time.Duration
import kz.bejiihiu.candyriya.plugin.EventBus
import kz.bejiihiu.candyriya.plugin.Plugin
import kz.bejiihiu.candyriya.plugin.PluginContext
import kz.bejiihiu.candyriya.plugin.PlayerJoinEvent
import kz.bejiihiu.candyriya.plugin.Subscribe
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * Minimal example showing every part of the candyriya Plugin API.
 *
 * Drop the built jar into `plugins/` and restart the proxy.
 */
public class HelloPlugin : Plugin {
    private lateinit var ctx: PluginContext

    override fun onLoad() {
        // called on plugin's own thread before proxy binds — good for config prep
        // can't use ctx here yet (it's null), keep it light
    }

    override fun onEnable(ctx: PluginContext) {
        this.ctx = ctx
        ctx.logger.info("Hello from {} v{}!", ctx.description.name, ctx.description.version)
        ctx.logger.info("Data dir: {}", ctx.dataDirectory.toAbsolutePath())

        // 1. events — annotation style
        ctx.events.register(ctx.description.id, JoinListener(ctx))

        // 1b. events — lambda style (auto-unregistered on disable)
        ctx.events.on(ctx.description.id, PlayerJoinEvent::class.java) { event ->
            ctx.logger.info("lambda listener: {} joined", event.player.username)
        }

        // 2. commands — runs on plugin thread automatically
        ctx.commands.register(
            "hello",
            object : kz.bejiihiu.candyriya.plugin.PluginCommand {
                override val permission: String? = "hello.use"
                override val description: String = "says hello"
                override val usage: String = "[name]"

                override fun execute(
                    source: kz.bejiihiu.candyriya.plugin.PluginCommandSource,
                    args: Array<String>,
                ) {
                    val target = args.firstOrNull() ?: source.name
                    source.sendMessage(Component.text("Hello, $target!", NamedTextColor.GREEN))
                    ctx.logger.info("{} used /hello {}", source.name, target)
                }
            }
        )

        // 3. scheduler — every task is tagged with plugin id, auto-cancelled on disable
        ctx.scheduler.execute {
            ctx.logger.info("runs immediately on plugin thread: {}", Thread.currentThread().name)
        }
        ctx.scheduler.delayed(Duration.ofSeconds(2)) {
            ctx.logger.info("delayed 2s task — still on plugin thread")
            ctx.server.broadcast(Component.text("Hello delayed broadcast!", NamedTextColor.YELLOW))
        }
        ctx.scheduler.repeating(Duration.ofSeconds(5), Duration.ofSeconds(30)) {
            ctx.logger.info("repeating: {} players online", ctx.server.getPlayerCount())
        }

        // 4. messaging channel
        ctx.messaging.registerChannel("hello:data")

        // 5. how to talk about Velocity bridge (future):
        // A single candyriya plugin can embed Velocity's api as compileOnly and
        // start its own Velocity PluginManager inside onEnable:
        //   val velocityLoader = VelocityBridge(ctx) // your class
        //   velocityLoader.loadJars(ctx.dataDirectory.resolve("velocity-plugins"))
        // Core does not import velocity types, so you can do this without forking proxy.
    }

    override fun onDisable() {
        ctx.logger.info("Bye from HelloPlugin!")
        // no need to cancel tasks / unregister listeners — PluginManager does it
    }

    // annotation listener must be a separate class (or this class) with @Subscribe
    public class JoinListener(private val ctx: PluginContext) {
        @Subscribe
        public fun onJoin(event: PlayerJoinEvent) {
            event.player.sendMessage(Component.text("Welcome ${event.player.username}!", NamedTextColor.AQUA))
            ctx.logger.info("JoinListener: welcome sent to {}", event.player.username)
        }
    }
}

