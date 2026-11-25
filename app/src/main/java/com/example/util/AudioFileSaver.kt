package com.example.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class ImportedAudioFile(
    val file: File,
    val mimeType: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long
)

object AudioFileSaver {
    private const val MAX_UPLOAD_BYTES = 18L * 1024L * 1024L // 18 MB (safely within Gemini 20 MB inline limit)

    private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "m4a", "aac", "ogg", "oga", "opus", "flac", "webm", "amr", "3gp", "3gpp", "aiff", "aif", "wma"
    )
    private val SUPPORTED_VIDEO_EXTENSIONS = setOf(
        "mp4", "mov", "mkv", "m4v"
    )

    /**
     * Saves an audio file directly to the device's Downloads/AI_Voice_Clone folder
     * using zero-permission MediaStore on Android 10+ (API 29+) and app external Downloads on API 24-28.
     */
    suspend fun saveAudioToDownloads(
        context: Context,
        sourcePath: String,
        displayTitle: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                return@withContext Result.failure(Exception("Audio file not found."))
            }

            val extension = sourceFile.extension.lowercase().ifBlank { "wav" }
            val mimeType = when (extension) {
                "mp3" -> "audio/mpeg"
                "m4a", "mp4" -> "audio/mp4"
                "ogg", "opus" -> "audio/ogg"
                "aac" -> "audio/aac"
                "flac" -> "audio/flac"
                else -> "audio/wav"
            }

            val cleanTitle = displayTitle
                .replace(Regex("[^A-Za-z0-9_\\- ]"), "")
                .trim()
                .replace(Regex("\\s+"), "_")
                .ifBlank { "AI_Voice" }
                .take(40)
            val fileName = "${cleanTitle}_${System.currentTimeMillis() % 100000}.$extension"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/AI_Voice_Clone")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(Exception("Could not create file in Downloads."))

                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                } ?: return@withContext Result.failure(Exception("Could not write to Downloads."))

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                Result.success("Downloads/AI_Voice_Clone/$fileName")
            } else {
                val downloadsDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "AI_Voice_Clone"
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                Result.success(destFile.absolutePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares an audio file reliably via FileProvider with proper URI permissions & ClipData.
     */
    fun shareAudioFile(context: Context, filePath: String, title: String): Result<Unit> {
        return try {
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                return Result.failure(Exception("Audio file not found."))
            }

            val mimeType = when (file.extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "m4a", "mp4" -> "audio/mp4"
                "ogg", "opus" -> "audio/ogg"
                "aac" -> "audio/aac"
                "flac" -> "audio/flac"
                else -> "audio/wav"
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TITLE, title)
                clipData = ClipData.newRawUri(title, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Audio").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports a user-selected voice file (audio or video clip) via zero-permission SAF URI
     * into app-private cache for AI Accent & Voice DNA analysis and cloning.
     * Supports MP3, WAV, M4A, AAC, OGG, OPUS (WhatsApp), FLAC, AMR, 3GP, WebM, MP4, MOV, MKV.
     */
    suspend fun importAudioFromUri(context: Context, uri: Uri): Result<ImportedAudioFile> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                var displayName = "uploaded_voice.mp3"
                var fileSize = -1L

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }

                if (fileSize > MAX_UPLOAD_BYTES) {
                    return@withContext Result.failure(
                        Exception("File is too large (max 18 MB). Please choose a 5–60 second voice or video clip.")
                    )
                }

                val rawMime = resolver.getType(uri)?.lowercase().orEmpty()
                val extFromName = displayName.substringAfterLast('.', "").lowercase()

                val isSupportedByExt = extFromName in SUPPORTED_AUDIO_EXTENSIONS || extFromName in SUPPORTED_VIDEO_EXTENSIONS
                val isSupportedByMime = rawMime.startsWith("audio/") || rawMime.startsWith("video/") || rawMime == "application/ogg"

                if (!isSupportedByExt && !isSupportedByMime && extFromName.isNotBlank()) {
                    return@withContext Result.failure(
                        Exception("Unsupported file type (.$extFromName). Please select an audio or video file (MP3, WAV, M4A, OPUS, OGG, AAC, FLAC, AMR, 3GP, MP4, WebM).")
                    )
                }

                val resolvedMime = when {
                    extFromName == "wav" -> "audio/wav"
                    extFromName == "mp3" -> "audio/mp3"
                    extFromName == "m4a" -> "audio/mp4"
                    extFromName == "aac" -> "audio/aac"
                    extFromName == "ogg" || extFromName == "oga" || extFromName == "opus" -> "audio/ogg"
                    extFromName == "flac" -> "audio/flac"
                    extFromName == "amr" -> "audio/amr"
                    extFromName == "3gp" || extFromName == "3gpp" -> "audio/3gpp"
                    extFromName == "aiff" || extFromName == "aif" -> "audio/aiff"
                    extFromName == "mp4" || extFromName == "m4v" -> "video/mp4"
                    extFromName == "mov" -> "video/quicktime"
                    extFromName == "mkv" -> "video/x-matroska"
                    extFromName == "webm" -> if (rawMime.startsWith("video/")) "video/webm" else "audio/webm"
                    rawMime.startsWith("audio/") && rawMime != "audio/*" -> rawMime
                    rawMime.startsWith("video/") && rawMime != "video/*" -> rawMime
                    rawMime == "application/ogg" -> "audio/ogg"
                    else -> "audio/mp3"
                }

                val resolvedExt = when {
                    extFromName.isNotBlank() && extFromName.length <= 5 -> extFromName
                    resolvedMime.contains("wav") -> "wav"
                    resolvedMime.contains("ogg") || resolvedMime.contains("opus") -> "ogg"
                    resolvedMime.contains("aac") -> "aac"
                    resolvedMime.contains("flac") -> "flac"
                    resolvedMime.contains("amr") -> "amr"
                    resolvedMime.contains("3gp") -> "3gp"
                    resolvedMime.contains("webm") -> "webm"
                    resolvedMime.startsWith("video/mp4") -> "mp4"
                    resolvedMime.contains("mp4") || resolvedMime.contains("m4a") -> "m4a"
                    else -> "mp3"
                }

                val destFile = File(
                    context.cacheDir,
                    "imported_voice_${System.currentTimeMillis()}.$resolvedExt"
                )

                var totalCopied = 0L
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            totalCopied += read
                            if (totalCopied > MAX_UPLOAD_BYTES) {
                                destFile.delete()
                                return@withContext Result.failure(
                                    Exception("File exceeds the 18 MB limit. Please select a shorter voice clip.")
                                )
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                } ?: return@withContext Result.failure(Exception("Could not read the selected file."))

                if (destFile.length() == 0L) {
                    destFile.delete()
                    return@withContext Result.failure(Exception("The selected file is empty."))
                }

                val durationMs = extractMediaDurationMs(destFile)

                Result.success(
                    ImportedAudioFile(
                        file = destFile,
                        mimeType = resolvedMime,
                        displayName = displayName,
                        durationMs = durationMs,
                        sizeBytes = destFile.length()
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun extractMediaDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
