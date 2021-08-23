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

package com.aliucord.gradle.entities

data class Author(
    val name: String,
    val id: Long,
)

data class PluginManifest(
    val pluginClassName: String,
    val name: String,
    val version: String,
    val description: String?,
    val authors: List<Author>,

    val updateUrl: String?,
    val changelog: String?,
    val changelogMedia: String?
)