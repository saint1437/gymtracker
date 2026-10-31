package com.sol.gymtracker.sync

import android.os.Build
import com.sol.gymtracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val title: String,
    val url: String,
    val notes: String,
) {
    fun isNewerThan(currentVersion: String): Boolean {
        fun parts(value: String): List<Int> = value.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val current = parts(currentVersion).plus(List(3) { 0 }).take(3)
        val latest = parts(tagName).plus(List(3) { 0 }).take(3)
        return latest.zip(current).firstOrNull { (remote, local) -> remote != local }?.let { it.first > it.second } ?: false
    }
}

object GitHubReleaseChecker {
    const val profileUrl = "https://github.com/saint1437"
    val repositoryUrl: String
        get() = BuildConfig.GITHUB_RELEASE_REPO.takeIf { it.isNotBlank() }?.let { "https://github.com/$it" } ?: profileUrl
    val isConfigured: Boolean
        get() = BuildConfig.GITHUB_RELEASE_REPO.isNotBlank()

    suspend fun latest(): GitHubRelease? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        runCatching {
            val latestReleaseUrl = "https://api.github.com/repos/${BuildConfig.GITHUB_RELEASE_REPO}/releases/latest"
            val connection = (URL(latestReleaseUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "GymTracker/${BuildConfig.VERSION_NAME} Android/${Build.VERSION.SDK_INT}")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() })
                GitHubRelease(
                    tagName = json.optString("tag_name"),
                    title = json.optString("name").ifBlank { json.optString("tag_name") },
                    url = json.optString("html_url", repositoryUrl),
                    notes = json.optString("body"),
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}