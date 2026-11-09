package com.example.flowfund_part2.data.repository

import retrofit2.HttpException
import java.io.IOException

/** Outcome of a refresh/sync, so callers can show a real message instead of guessing. */
sealed interface SyncResult {
    data object Success : SyncResult
    data class Failure(val message: String, val isAuth: Boolean = false) : SyncResult
}

/** Turns a network/HTTP exception into a user-facing [SyncResult.Failure]. Never invents data. */
internal fun Throwable.toFailure(): SyncResult.Failure = when (this) {
    is HttpException -> when (code()) {
        401 -> SyncResult.Failure("Your session expired. Please sign in again.", isAuth = true)
        in 500..599 -> SyncResult.Failure("The server had a problem. Please try again shortly.")
        else -> SyncResult.Failure("The server rejected the request (${code()}).")
    }
    is IOException -> SyncResult.Failure("No connection to the server. Your changes are saved on this device.")
    else -> SyncResult.Failure("Something went wrong: ${message ?: javaClass.simpleName}")
}
