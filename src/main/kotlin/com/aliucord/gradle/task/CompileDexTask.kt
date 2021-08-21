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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.compile.JavaCompile

abstract class CompileDexTask : Exec() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    override fun exec() {
        val compileTask = project.tasks.getByName("compileDebugJavaWithJavac") as JavaCompile

        val android = project.extensions.getByName("android") as BaseExtension

        executable =
            android.sdkDirectory.resolve("build-tools").resolve(android.buildToolsVersion).resolve("d8").absolutePath

        args("--output")
        val outputFile = outputFile.get().asFile.parent
        args(outputFile)

        args(
            compileTask.destinationDirectory.asFile.get().walkTopDown().filter { it.extension == "class" }
                .map { it.absolutePath }.asIterable()
        )

        super.exec()

        logger.lifecycle("Compiled dex to $outputFile")
    }
}