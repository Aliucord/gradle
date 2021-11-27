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
import org.gradle.api.tasks.TaskAction
import se.vidstige.jadb.AdbServerLauncher
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.Subprocess
import java.nio.charset.StandardCharsets

abstract class UninstallWithAdbTask : DefaultTask() {
    @TaskAction
    fun uninstallWithAdb() {
        val extension = project.extensions.getAliucord()
        val android = project.extensions.getByName("android") as BaseExtension

        AdbServerLauncher(Subprocess(), android.adbExecutable.absolutePath).launch()
        val jadbConnection = JadbConnection()
        val devices = jadbConnection.devices

        require(devices.size == 1) {
            "Only one ADB device should be connected, but ${devices.size} were!"
        }

        val make = project.tasks.getByName("make") as AbstractCopyTask
        var file = make.outputs.files.singleFile

        if (extension.projectType.get() == ProjectType.INJECTOR)
            file = file.resolve("Injector.dex")

        var path = "/storage/emulated/0/Aliucord/"

        if (extension.projectType.get() == ProjectType.PLUGIN)
            path += "plugins/"

        val device = devices[0]
        device.execute("rm", path + file.name)

        if (extension.projectType.get() != ProjectType.INJECTOR) {
            val args =
                arrayListOf("start", "-S", "-n", "com.aliucord/com.discord.app.AppActivity\$Main")

            val response = String(
                device.executeShell("am", *args.toTypedArray()).readAllBytes(),
                StandardCharsets.UTF_8
            )

            if (response.contains("Error"))
                logger.error(response)
        }

        logger.lifecycle("Deleted $file from ${device.serial}")
    }
}