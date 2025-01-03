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
import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import se.vidstige.jadb.*

abstract class DeployWithAdbTask : DefaultTask() {
    @get:Input
    @set:Option(option = "wait-for-debugger", description = "Enables debugging flag when starting the discord activity")
    var waitForDebugger: Boolean = false

    @TaskAction
    fun deployWithAdb() {
        val extension = project.extensions.getAliucord()
        val android = project.extensions.getByName("android") as BaseExtension

        AdbServerLauncher(Subprocess(), android.adbExecutable.absolutePath).launch()
        val jadbConnection = JadbConnection()
        val devices = jadbConnection.devices.filter {
            try {
                it.state == JadbDevice.State.Device
            } catch (e: JadbException) {
                false
            }
        }

        require(devices.size == 1) {
            "Only one ADB device should be connected, but ${devices.size} were!"
        }

        val device = devices[0]

        val make = project.tasks.getByName("make") as AbstractCopyTask

        var file = make.outputs.files.singleFile

        if (extension.projectType.get() == ProjectType.INJECTOR) {
            file = file.resolve("Injector.dex")
        }

        val remotePath = when (extension.projectType.get()) {
            ProjectType.PLUGIN -> "/storage/emulated/0/Aliucord/plugins/${file.name}"
            ProjectType.CORE -> "/storage/emulated/0/Aliucord/Aliucord.zip"
            ProjectType.INJECTOR -> "/storage/emulated/0/Android/data/com.aliucord.manager/cache/injector/${project.version}.custom.dex"
        }
        device.push(file, RemoteFile(remotePath))

        val activityName = when (extension.projectType.get()) {
            ProjectType.PLUGIN, ProjectType.CORE -> "com.aliucord/com.discord.app.AppActivity\$Main"
            ProjectType.INJECTOR -> "com.aliucord.manager/com.aliucord.manager.MainActivity"
        }

        val args = arrayListOf("start", "-S", "-n", activityName)
        if (waitForDebugger) args += "-D"

        val response = device.executeShell("am", *args.toTypedArray())
            .readAllBytes()
            .decodeToString()

        if (response.contains("Error")) {
            logger.error(response)
        }

        logger.lifecycle("Deployed $file to ${device.serial}")
    }
}