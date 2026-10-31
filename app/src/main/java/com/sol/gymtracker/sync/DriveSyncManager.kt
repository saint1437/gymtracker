package com.sol.gymtracker.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.sol.gymtracker.BuildConfig
import com.sol.gymtracker.data.repo.WorkoutRepository
import com.sol.gymtracker.domain.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class DriveSyncManager(private val context: Context, private val repository: WorkoutRepository) {
    private val jsonFactory = GsonFactory.getDefaultInstance()

    fun signInIntent(): Intent? {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return null
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) builder.requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        return GoogleSignIn.getClient(context, builder.build()).signInIntent
    }

    fun accountFromResult(data: Intent?): GoogleSignInAccount? = runCatching {
        GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
    }.getOrNull()

    suspend fun sync(account: GoogleSignInAccount): SyncResult = withContext(Dispatchers.IO) {
        val drive = drive(account)
        val folderId = ensureFolder(drive)
        val remote = findHistory(drive, folderId)
        val export = ExportManager(context, repository)
        var localPayload = export.json()
        val lastSyncAt = com.sol.gymtracker.data.db.WorkoutDatabase.get(context).settings().get()?.lastSyncAt ?: 0L

        if (remote != null && (remote.modifiedTime?.value ?: 0L) > lastSyncAt) {
            val remotePayload = download(drive, remote.id)
            if (remotePayload.isNotBlank()) {
                // Internal backup is created before remote data is merged. Merge is UUID + updatedAt based.
                export.writeInternalBackup(localPayload)
                repository.restoreFromJson(remotePayload)
                localPayload = export.json()
            }
        }

        val localCsv = export.csv()
        upsertFile(drive, folderId, HISTORY_FILE, "application/json", localPayload, remote)
        val remoteCsv = findFile(drive, folderId, HISTORY_CSV_FILE)
        upsertFile(drive, folderId, HISTORY_CSV_FILE, "text/csv", localCsv, remoteCsv)
        val db = com.sol.gymtracker.data.db.WorkoutDatabase.get(context)
        db.settings().save((db.settings().get() ?: com.sol.gymtracker.data.db.AppSettingsEntity()).copy(driveAccount = account.email, driveFolderId = folderId, syncEnabled = true, lastSyncAt = System.currentTimeMillis()))
        for (queued in repository.pendingSync()) repository.markSynced(queued.id)
        SyncResult(true, "Синхронизация завершена")
    }

    private fun drive(account: GoogleSignInAccount): Drive {
        val selectedAccount = account.account ?: error("Google account is unavailable")
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE)).setSelectedAccount(selectedAccount)
        return Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential).setApplicationName("GymTracker").build()
    }

    private fun ensureFolder(drive: Drive): String {
        val query = "name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val existing = drive.files().list().setQ(query).setSpaces("drive").setFields("files(id,name)").execute().files
        return existing.firstOrNull()?.id ?: drive.files().create(File().setName(FOLDER_NAME).setMimeType("application/vnd.google-apps.folder")).setFields("id").execute().id
    }

    private fun findHistory(drive: Drive, folderId: String): File? {
        return findFile(drive, folderId, HISTORY_FILE)
    }

    private fun findFile(drive: Drive, folderId: String, name: String): File? {
        val query = "name = '$name' and '$folderId' in parents and trashed = false"
        return drive.files().list().setQ(query).setSpaces("drive").setFields("files(id,name,modifiedTime)").execute().files.firstOrNull()
    }

    private fun upsertFile(drive: Drive, folderId: String, name: String, mimeType: String, content: String, remote: File?) {
        val metadata = File().setName(name).setMimeType(mimeType).setParents(listOf(folderId))
        val media = ByteArrayContent(mimeType, content.toByteArray(Charsets.UTF_8))
        if (remote == null) drive.files().create(metadata, media).setFields("id,modifiedTime").execute()
        else drive.files().update(remote.id, metadata, media).setFields("id,modifiedTime").execute()
    }

    private fun download(drive: Drive, id: String): String {
        val output = ByteArrayOutputStream()
        drive.files().get(id).executeMediaAndDownloadTo(output)
        return output.toString(Charsets.UTF_8.name())
    }

    companion object {
        const val FOLDER_NAME = "Тренировки"
        const val HISTORY_FILE = "workout_history.json"
        const val HISTORY_CSV_FILE = "workout_history.csv"
    }
}

data class SyncResult(val success: Boolean, val message: String)