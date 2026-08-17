package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.storage.storage
import io.lunosfer.dreamap.data.model.SaveVisionVideoRequest
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import java.io.File
import kotlin.random.Random

/**
 * lib/uploadVisionVideo.js + pages/api/goals/save-vision-video.js'in native
 * karşılığı. Goal her zaman ÖNCEDEN var (CreateVisionScreen zaten
 * oluşturuyor) — bu repository sadece export edilen dosyayı Storage'a
 * yükleyip o goal'a bağlıyor.
 */
class VideoEditorRepository {
    private val bucket = supabaseClient.storage.from("goal-videos")
    private val api = NetworkModule.api

    companion object {
        private const val MAX_BYTES = 150 * 1024 * 1024 // 150MB — web ile aynı üst sınır
    }

    suspend fun attachToGoal(file: File, userId: String, goalId: String): Result<String> = runCatching {
        if (file.length() > MAX_BYTES) throw IllegalStateException("file_too_large")
        val uniquePart = "${System.currentTimeMillis()}-${Random.nextInt(100000, 999999)}"
        val path = "$userId/$goalId/$uniquePart.mp4"
        bucket.upload(path, file.readBytes())
        val publicUrl = bucket.publicUrl(path)
        api.saveVisionVideo(SaveVisionVideoRequest(goalId = goalId, videoUrl = publicUrl))
        publicUrl
    }
}
