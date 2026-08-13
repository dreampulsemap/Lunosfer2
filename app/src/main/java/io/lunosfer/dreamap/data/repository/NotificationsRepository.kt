package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.data.network.NetworkModule

class NotificationsRepository {
    private val api = NetworkModule.api

    suspend fun getNotifications(): Result<NotificationsResponse> = runCatching {
        api.getNotifications()
    }

    suspend fun markNotificationsRead(notificationId: String? = null): Result<Boolean> = runCatching {
        val res = api.markNotificationsRead(MarkNotificationReadInput(notificationId = notificationId))
        if (!res.success && res.error != null) {
            throw Exception(res.error)
        }
        res.success
    }
}
