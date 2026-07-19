package com.project.myscale.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.project.myscale.BodyTrackApplication
import com.project.myscale.data.model.BackupInterval
import com.project.myscale.util.CsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Writes a CSV backup into the user-chosen SAF folder. Because the folder is
 * picked via the system document tree, it can live anywhere — device storage or
 * a cloud provider's synced folder (Nextcloud, Drive, Dropbox, ...) — without
 * the app needing any cloud SDK.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (runBackup(applicationContext)) {
            BackupResult.SUCCESS, BackupResult.NOT_CONFIGURED -> Result.success()
            BackupResult.FOLDER_INACCESSIBLE -> Result.failure()
            BackupResult.WRITE_FAILED -> Result.retry()
        }
    }

    enum class BackupResult {
        SUCCESS,
        NOT_CONFIGURED,
        FOLDER_INACCESSIBLE,
        WRITE_FAILED
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "auto_backup"
        private const val FILE_PREFIX = "dailyscale_backup_"
        private const val KEEP_BACKUPS = 10

        /** Shared by the periodic worker and the "back up now" settings action. */
        suspend fun runBackup(context: Context): BackupResult = withContext(Dispatchers.IO) {
            val app = context.applicationContext as BodyTrackApplication
            val uriString = app.preferencesManager.backupFolderUri.first()
                ?: return@withContext BackupResult.NOT_CONFIGURED

            val tree = DocumentFile.fromTreeUri(context, uriString.toUri())
            if (tree == null || !tree.canWrite()) {
                return@withContext BackupResult.FOLDER_INACCESSIBLE
            }

            val entries = app.repository.getAllEntriesForExport()
            val fileName = "$FILE_PREFIX${LocalDate.now()}.csv"

            try {
                // Overwrite today's backup instead of stacking duplicates
                tree.findFile(fileName)?.delete()
                val file = tree.createFile("text/csv", fileName)
                    ?: return@withContext BackupResult.WRITE_FAILED
                context.contentResolver.openOutputStream(file.uri)?.use { os ->
                    CsvExporter.export(entries, os)
                } ?: return@withContext BackupResult.WRITE_FAILED

                pruneOldBackups(tree)
                BackupResult.SUCCESS
            } catch (_: Exception) {
                BackupResult.WRITE_FAILED
            }
        }

        private fun pruneOldBackups(tree: DocumentFile) {
            tree.listFiles()
                .filter { it.name?.startsWith(FILE_PREFIX) == true }
                .sortedByDescending { it.name }
                .drop(KEEP_BACKUPS)
                .forEach { it.delete() }
        }

        fun schedule(context: Context, interval: BackupInterval) {
            val workManager = WorkManager.getInstance(context)
            if (interval == BackupInterval.OFF) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<BackupWorker>(interval.days, TimeUnit.DAYS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
