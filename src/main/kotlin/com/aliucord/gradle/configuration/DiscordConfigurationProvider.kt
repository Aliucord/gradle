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

package com.aliucord.gradle.configuration

import com.aliucord.gradle.DiscordInfo
import com.aliucord.gradle.copyInputStreamToFile
import com.aliucord.gradle.getAliucord
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.BaseDexFileReader
import com.googlecode.d2j.reader.MultiDexFileReader
import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import java.lang.Integer.parseInt
import java.net.URL
import java.nio.file.Files

class DiscordConfigurationProvider : IConfigurationProvider {
    companion object {
        private var aliucordSnapshot: Int? = null
    }

    override val name: String
        get() = "discord"

    override fun provide(project: Project, dependency: Dependency) {
        val version = if (dependency.version == "aliucord-SNAPSHOT") run fetch@ {
            if (aliucordSnapshot != null) return@fetch aliucordSnapshot!!
            project.logger.lifecycle("Fetching discord version")
            val data = JsonSlurper().parse(URL("https://raw.githubusercontent.com/Aliucord/Aliucord/builds/data.json")) as Map<*, *>
            val versionCode = parseInt(data["versionCode"] as String)
            project.logger.lifecycle("Fetched discord version: $versionCode")
            versionCode.also { aliucordSnapshot = versionCode }
        } else parseInt(dependency.version)

        val extension = project.extensions.getAliucord()
        val discord = DiscordInfo(extension, version)
        extension.discord = discord

        discord.cache.mkdirs()

        if (!discord.apkFile.exists()) {
            project.logger.lifecycle("Downloading discord apk")

            val url = URL("https://aliucord.tk/download/discord?v=${discord.version}")
            discord.apkFile.copyInputStreamToFile(url.openStream())
        }

        if (!discord.jarFile.exists()) {
            project.logger.lifecycle("Converting discord apk to jar")

            val reader: BaseDexFileReader = MultiDexFileReader.open(Files.readAllBytes(discord.apkFile.toPath()))
            Dex2jar.from(reader).topoLogicalSort().skipDebug(false).noCode(true).to(discord.jarFile.toPath())
        }

        project.dependencies.add("implementation", project.files(discord.jarFile))
    }
}