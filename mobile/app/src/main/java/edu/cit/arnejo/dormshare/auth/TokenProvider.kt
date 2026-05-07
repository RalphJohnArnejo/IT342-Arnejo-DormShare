package edu.cit.arnejo.dormshare.auth

object TokenProvider {
    // In-memory token holder. Persist to preferences if needed later.
    @Volatile
    var token: String? = null
}
