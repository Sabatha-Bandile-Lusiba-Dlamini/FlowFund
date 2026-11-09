package com.example.flowfund_part2.data.local

/**
 * Where a locally stored row stands relative to the server.
 * Room is the single source of truth for the UI; the repositories move rows between these states
 * (offline-first pattern, Android Developers, 2026a).
 */
object SyncState {
    /** Identical to the server copy. */
    const val SYNCED = "SYNCED"

    /** Created or edited on the phone and not yet accepted by the API. */
    const val PENDING = "PENDING"

    /** Deleted on the phone; kept (hidden) until the API confirms the deletion. */
    const val DELETED = "DELETED"
}

/*
REFERENCES

Android Developers (2026a) 'Build an offline-first app'.
Available at: https://developer.android.com/topic/architecture/data-layer/offline-first
(Accessed: 4 November 2026).
*/
