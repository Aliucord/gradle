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

class Links : HashMap<String?, String?>() {
    companion object {
        var GITHUB = "github"
        var SOURCE = "source"
    }

    var github: String?
        get() = get(GITHUB)
        set(value) {
            put(GITHUB, value)
        }

    var source: String?
        get() {
            if (containsKey(GITHUB)) {
                return github
            }

            return get(SOURCE)
        }
        set(value) {
            put(SOURCE, value)
        }
}

data class PluginManifest(
    val pluginClassName: String,
    val name: String,
    val version: String,
    val description: String?,
    val authors: List<Author>,
    val links: Links,

    val updateUrl: String?,
    val changelog: String?,
    val changelogMedia: String?
)