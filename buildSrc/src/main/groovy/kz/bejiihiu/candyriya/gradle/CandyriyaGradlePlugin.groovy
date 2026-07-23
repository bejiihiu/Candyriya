package kz.bejiihiu.candyriya.gradle

import kz.bejiihiu.candyriya.gradle.extension.CandyriyaExtension
import kz.bejiihiu.candyriya.gradle.runnable.FileDownloader
import kz.bejiihiu.candyriya.gradle.runnable.SpigotBuilder
import kz.bejiihiu.candyriya.gradle.tasks.ProcessMappingTask
import kz.bejiihiu.candyriya.gradle.tasks.RemapSpigotTask
import kz.bejiihiu.candyriya.gradle.tasks.RenameJarTask
import net.fabricmc.loom.LoomGradlePlugin
import net.fabricmc.loom.configuration.mods.dependency.LocalMavenHelper
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.apache.commons.io.IOUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class CandyriyaGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def candyriya = project.extensions.create('candyriya', CandyriyaExtension, project)

        def candyriyaRepo = candyriya.cacheDir.resolve('candyriya_repo')
        project.repositories.maven {
            name = 'Candyriya Spigot Repo'
            url = candyriyaRepo
        }

        def mappingsDir = candyriya.cacheDir.resolve('candyriya_cache/mappings')
        def forgeMappings = mappingsDir.resolve('bukkit_srg.srg').toFile()
        def forgeInheritance = mappingsDir.resolve('inheritanceMap.txt').toFile()
        def reobfMappings = mappingsDir.resolve('reobf_bukkit.srg').toFile()
        def neoforgeMappings = mappingsDir.resolve('bukkit_moj.srg').toFile()
        def fabricMappings = mappingsDir.resolve('bukkit_intermediary.srg').toFile()
        def fabricInheritance = mappingsDir.resolve('inheritanceMap_intermediary.txt').toFile()
        candyriya.mappingsConfiguration.bukkitToForge = forgeMappings
        candyriya.mappingsConfiguration.reobfBukkitPackage = reobfMappings
        candyriya.mappingsConfiguration.bukkitToForgeInheritance = forgeInheritance
        candyriya.mappingsConfiguration.bukkitToNeoForge = neoforgeMappings
        candyriya.mappingsConfiguration.bukkitToFabric = fabricMappings
        candyriya.mappingsConfiguration.bukkitToFabricInheritance = fabricInheritance

        project.tasks.register('relocateCraftBukkit', RenameJarTask) {
            it.dependsOn project.tasks.remapJar
            inputJar.set project.tasks.remapJar.archiveFile
            archiveClassifier.set 'relocated'
            mappings = candyriya.mappingsConfiguration.reobfBukkitPackage
        }
        project.tasks.build.dependsOn('relocateCraftBukkit')

        project.afterEvaluate {
            setupSpigot(project, candyriyaRepo)
        }
    }

    private static def setupSpigot(Project project, Path candyriyaRepo) {
        def candyriya = project.extensions.getByName('candyriya') as CandyriyaExtension


        def mappingsDir = candyriya.cacheDir.resolve('candyriya_cache/mappings')

        def spigotDeps = candyriyaRepo.resolve("kz/bejiihiu/candyriya/generated/spigot/${candyriya.mcVersion}")
        def spigotMapped = spigotDeps.resolve("spigot-${candyriya.mcVersion}-mapped.jar")
        def spigotDeobf = spigotDeps.resolve("spigot-${candyriya.mcVersion}-deobf.jar")

        def buildMeta = candyriya.cacheDir.resolve('spigot_version.json')
        def rev = candyriya.mcVersion
        if (candyriya.spigotReversion) {
            rev = candyriya.spigotReversion
        }

        project.logger.lifecycle("Setup for Spigot ${candyriya.mcVersion}(${candyriya.spigotReversion})")
        def newBuildMeta = IOUtils.toString(new URI("https://hub.spigotmc.org/versions/${rev}.json").toURL(), StandardCharsets.UTF_8)
        if (Files.exists(buildMeta)) {
            var built = Files.readString(buildMeta)
            if (built == newBuildMeta) {
                if (candyriya.mappingsConfiguration.areMappingsExist()
                        && Files.exists(spigotDeobf)) {
                    project.logger.lifecycle(":spigot build cache valid, using it")
                    project.logger.debug(built)
                    return
                }
            }
        }

        def buildSpigotWorkDir = candyriya.cacheDir.resolve('candyriya_cache/buildtools')

        FileUtils.deleteDirectory(buildSpigotWorkDir.toFile())
        Files.createDirectories(buildSpigotWorkDir)

        project.logger.lifecycle(":step1 download build tools")
        def buildToolsJar = buildSpigotWorkDir.resolve('BuildTools.jar')
        def downloadBuildTools = new FileDownloader("https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar", buildToolsJar)
        downloadBuildTools.run()

        project.logger.lifecycle(":step2 build spigot")
        def spigotBuilder = project.getObjects().newInstance(SpigotBuilder)
        spigotBuilder.buildToolsJar = buildToolsJar
        spigotBuilder.workDir = buildSpigotWorkDir
        spigotBuilder.outputDir = spigotDeps
        spigotBuilder.minecraftVersion = candyriya.mcVersion
        spigotBuilder.reversion = candyriya.spigotReversion
        spigotBuilder.run()

        new LocalMavenHelper("kz.bejiihiu.candyriya.generated", "spigot", candyriya.mcVersion, null, candyriyaRepo).savePom()

        project.logger.lifecycle(":step3 process mappings")
        def processMapping = new ProcessMappingTask(project)
        processMapping.buildData = new File(buildSpigotWorkDir.toFile(), 'BuildData')
        processMapping.mcVersion = candyriya.mcVersion
        processMapping.bukkitVersion = candyriya.bukkitVersion
        processMapping.outDir = mappingsDir.toFile()
        processMapping.inJar = spigotBuilder.outputJar.toFile()
        processMapping.run()

        project.logger.lifecycle(":step4 remap spigot jar")
        def remapSpigot = new RemapSpigotTask(project)
        remapSpigot.ssJar = new File(buildSpigotWorkDir.toFile(), 'BuildData/bin/SpecialSource.jar')
        remapSpigot.inJar = spigotBuilder.outputJar.toFile()
        remapSpigot.inSrg = new File(processMapping.outDir, 'bukkit_srg.srg')
        remapSpigot.inSrgToStable = new File(processMapping.outDir, "srg_to_named.srg")
        remapSpigot.inheritanceMap = new File(processMapping.outDir, 'inheritanceMap.txt')
        remapSpigot.outJar = project.file(spigotMapped)
        remapSpigot.outDeobf = project.file(spigotDeobf)
        remapSpigot.inAt = candyriya.accessTransformer
        remapSpigot.bukkitVersion = candyriya.bukkitVersion
        remapSpigot.inExtraSrg = candyriya.extraMapping
        remapSpigot.run()

        Files.writeString(buildMeta, newBuildMeta)
    }
}
