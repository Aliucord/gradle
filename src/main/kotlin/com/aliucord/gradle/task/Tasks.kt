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
import com.aliucord.gradle.entities.PluginManifest
import com.aliucord.gradle.getAliucord
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.ProcessLibraryManifest
import groovy.json.JsonBuilder
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.AbstractCompile

const val TASK_GROUP = "aliucord"

fun registerTasks(project: Project) {
    val extension = project.extensions.getAliucord()
    val intermediates = project.buildDir.resolve("intermediates")

    if (project.rootProject.tasks.findByName("generateUpdaterJson") == null) {
        project.rootProject.tasks.register("generateUpdaterJson", GenerateUpdaterJsonTask::class.java) {
            it.group = TASK_GROUP

            it.outputs.upToDateWhen { false }

            it.outputFile.set(it.project.buildDir.resolve("updater.json"))
        }
    }

    project.tasks.register("genSources", GenSourcesTask::class.java) {
        it.group = TASK_GROUP
    }

    val pluginClassFile = intermediates.resolve("pluginClass")

    val compileDex = project.tasks.register("compileDex", CompileDexTask::class.java) {
        it.group = TASK_GROUP

        it.pluginClassFile.set(pluginClassFile)

        for (name in arrayOf("compileDebugJavaWithJavac", "compileDebugKotlin")) {
            val task = project.tasks.findByName(name) as AbstractCompile?
            if (task != null) {
                it.dependsOn(task)
                it.input.from(task.destinationDirectory)
            }
        }

        it.outputFile.set(intermediates.resolve("classes.dex"))
    }

    val compileResources = project.tasks.register("compileResources", CompileResourcesTask::class.java) {
        it.group = TASK_GROUP

        val processManifestTask = project.tasks.getByName("processDebugManifest") as ProcessLibraryManifest
        it.dependsOn(processManifestTask)

        val android = project.extensions.getByName("android") as BaseExtension
        it.input.set(android.sourceSets.getByName("main").res.srcDirs.single())
        it.manifestFile.set(processManifestTask.manifestOutputFile)

        it.outputFile.set(intermediates.resolve("res.apk"))

        it.doLast { _ ->
            val resApkFile = it.outputFile.asFile.get()

            if (resApkFile.exists()) {
                project.tasks.named("make", AbstractCopyTask::class.java) {
                    it.from(project.zipTree(resApkFile)) { copySpec ->
                        copySpec.exclude("AndroidManifest.xml")
                    }
                }
            }
        }
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
                val manifestFile = intermediates.resolve("manifest.json")

                it.from(manifestFile)
                it.doFirst {
                    require(project.version != "unspecified") {
                        "No version is set"
                    }

                    if (extension.pluginClassName == null) {
                        if (pluginClassFile.exists()) {
                            extension.pluginClassName = pluginClassFile.readText()
                        }
                    }

                    require(extension.pluginClassName != null) {
                        "No plugin class found, make sure your plugin class is annotated with @AliucordPlugin"
                    }

                    manifestFile.writeText(
                        JsonBuilder(
                            PluginManifest(
                                pluginClassName = extension.pluginClassName!!,
                                name = project.name,
                                version = project.version.toString(),
                                description = project.description,
                                authors = extension.authors.get(),
                                updateUrl = extension.updateUrl.orNull,
                                changelog = extension.changelog.orNull,
                                changelogMedia = extension.changelogMedia.orNull
                            )
                        ).toPrettyString()
                    )
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

                zip.dependsOn(compileResources.get())
                zip.archiveBaseName.set(project.name)
                zip.archiveVersion.set("")
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