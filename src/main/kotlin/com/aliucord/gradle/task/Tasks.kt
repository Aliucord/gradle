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

import com.aliucord.gradle.ProjectType
import com.aliucord.gradle.getAliucord
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.AbstractCompile

const val TASK_GROUP = "aliucord"

fun registerTasks(project: Project) {
    val extension = project.extensions.getAliucord()

    project.tasks.register("genSources", GenSourcesTask::class.java) {
        it.group = TASK_GROUP
    }

    val compileDex = project.tasks.register("compileDex", CompileDexTask::class.java) {
        it.group = TASK_GROUP

        for (name in arrayOf("compileDebugJavaWithJavac", "compileDebugKotlin")) {
            val task = project.tasks.getByName(name) as AbstractCompile?
            if (task != null) {
                it.dependsOn(task)
                it.input.from(task.destinationDirectory)
            }
        }

        it.outputFile.set(project.buildDir.resolve("intermediates").resolve("classes.dex"))
    }

    project.afterEvaluate {
        project.tasks.register(
            "make",
            if (extension.projectType.get() == ProjectType.INJECTOR) Copy::class.java else Zip::class.java
        )
        {
            it.group = TASK_GROUP
            val compileDexTask = compileDex.get()
            it.dependsOn(compileDexTask)

            if (extension.projectType.get() == ProjectType.PLUGIN) {
                val nameFile = project.buildDir.resolve("intermediates").resolve("ac-plugin")

                it.from(nameFile)
                it.doFirst {
                    nameFile.writeText(project.name)
                }
            }

            it.from(compileDexTask.outputFile)

            if (extension.projectType.get() == ProjectType.INJECTOR) {
                it.into(project.buildDir)
                it.rename { return@rename "Injector.dex" }

                it.doLast { task ->
                    task.logger.lifecycle("Copied Injector.dex to ${project.buildDir}")
                }
            } else {
                val zip = it as Zip
                zip.archiveBaseName.set(project.name)
                zip.destinationDirectory.set(project.buildDir)

                it.doLast { task ->
                    task.logger.lifecycle("Made Aliucord package at ${task.outputs.files.singleFile}")
                }
            }
        }

        project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) {
            it.group = TASK_GROUP
            it.dependsOn("make")
        }
    }
}