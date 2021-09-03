/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.aliucord.gradle.task

import com.aliucord.gradle.AliucordExtension
import com.aliucord.gradle.getAliucord
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions.ErrorFormatMode
import com.android.builder.dexing.ClassFileInputs
import com.android.builder.dexing.DexArchiveBuilder
import com.android.builder.dexing.DexParameters
import com.android.builder.dexing.r8.ClassFileProviderFactory
import com.google.common.io.Closer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

abstract class CompileDexTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    val input: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val pluginClassFile: RegularFileProperty

    @Suppress("UnstableApiUsage")
    @TaskAction
    fun compileDex() {
        val android = project.extensions.getByName("android") as BaseExtension

        val dexOutputDir = outputFile.get().asFile.parentFile

        Closer.create().use { closer ->
            val dexBuilder = DexArchiveBuilder.createD8DexBuilder(
                DexParameters(
                    minSdkVersion = android.defaultConfig.maxSdkVersion ?: 24,
                    debuggable = true,
                    dexPerClass = false,
                    withDesugaring = true,
                    desugarBootclasspath = ClassFileProviderFactory(android.bootClasspath.map(File::toPath))
                        .also { closer.register(it) },
                    desugarClasspath = ClassFileProviderFactory(listOf<Path>()).also { closer.register(it) },
                    coreLibDesugarConfig = null,
                    coreLibDesugarOutputKeepRuleFile = null,
                    messageReceiver = MessageReceiverImpl(
                        ErrorFormatMode.HUMAN_READABLE,
                        LoggerFactory.getLogger(CompileDexTask::class.java)
                    )
                )
            )

            val fileStreams =
                input.map { input -> ClassFileInputs.fromPath(input.toPath()).use { it.entries { _, _ -> true } } }
                    .toTypedArray()

            Arrays.stream(fileStreams).flatMap { it }
                .use { classesInput ->
                    val files = classesInput.collect(Collectors.toList())

                    dexBuilder.convert(
                        files.stream(),
                        dexOutputDir.toPath()
                    )

                    for (file in files) {
                        val reader = ClassReader(file.readAllBytes())

                        reader.accept(object : ClassVisitor(Opcodes.ASM9) {
                            override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                                if (descriptor == "Lcom/aliucord/annotations/AliucordPlugin;") {
                                    val aliucord = project.extensions.getAliucord()

                                    require(aliucord.pluginClassName == null) {
                                        "Only 1 active plugin class per project is supported"
                                    }

                                    aliucord.pluginClassName = reader.className.replace('/', '.')
                                        .also { pluginClassFile.asFile.orNull?.writeText(it) }

                                    return PluginAnnotationVisitor(aliucord)
                                }

                                return null
                            }
                        }, 0)
                    }
                }
        }

        logger.lifecycle("Compiled dex to ${outputFile.get()}")
    }
}


class PluginAnnotationVisitor(private val ext: AliucordExtension) : AnnotationVisitor(Opcodes.ASM9) {
    override fun visit(name: String, v: Any) {
        if (v is String && v.isNotBlank()) {
            when (name) {
                "version" -> ext.annotatedVersion = v
                "description" -> ext.annotatedDescription = v
                "changelog" -> ext.annotatedChangelog = v
                "changelogMedia" -> ext.annotatedChangelogMedia = v
            }
        }
        super.visit(name, v)
    }
}