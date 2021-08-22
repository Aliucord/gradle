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

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions.ErrorFormatMode
import com.android.builder.dexing.ClassFileInputs
import com.android.builder.dexing.DexArchiveBuilder
import com.android.builder.dexing.DexParameters
import com.android.builder.dexing.r8.ClassFileProviderFactory
import com.google.common.io.Closer
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

abstract class CompileDexTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @Suppress("UnstableApiUsage")
    @TaskAction
    fun compileDex() {
        val android = project.extensions.getByName("android") as BaseExtension
        val compileTask = project.tasks.getByName("compileDebugJavaWithJavac") as JavaCompile

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

            ClassFileInputs.fromPath(compileTask.destinationDirectory.asFile.get().toPath()).use { classFileInput ->
                classFileInput.entries { _, _ -> true }.use { classesInput ->
                    dexBuilder.convert(
                        classesInput,
                        dexOutputDir.toPath()
                    )
                }
            }
        }

        logger.lifecycle("Compiled dex to ${outputFile.get()}")
    }
}