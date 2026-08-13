package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.data.network.NetworkModule

class FriendsRepository {
    private val api = NetworkModule.api

    suspend fun getFriendsList(userId: String, type: String? = null): Result<List<Friendship>> = runCatching {
        api.getFriendsList(userId = userId, type = type).friendships
    }

    suspend fun sendFriendRequest(userId: String, friendId: String): Result<FriendRequestResponse> = runCatching {
        val res = api.sendFriendRequest(FriendRequestInput(userId = userId, friendId = friendId))
        if (!res.success && res.error != null) {
            throw Exception(res.error ?: res.message ?: "Takip isteği gönderilemedi")
        }
        res
    }

    suspend fun respondToFriendRequest(friendshipId: String, userId: String, action: String): Result<FriendRespondResponse> = runCatching {
        val res = api.respondToFriendRequest(FriendRespondInput(friendshipId = friendshipId, userId = userId, action = action))
        if (!res.success && res.error != null) {
            throw Exception(res.error ?: res.message ?: "İşlem başarısız")
        }
        res
    }

    suspend fun searchFriends(query: String, userId: String): Result<List<UserSearchResult>> = runCatching {
        api.searchFriends(query = query, userId = userId).users
    }

    suspend fun getPublicProfile(userId: String, page: Int = 0): Result<PublicProfileResponse> = runCatching {
        val res = api.getPublicProfile(userId = userId, page = page)
        if (res.error != null) {
            throw Exception(res.error)
        }
        res
    }
}
