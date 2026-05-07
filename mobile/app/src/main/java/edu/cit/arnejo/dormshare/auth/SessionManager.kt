package edu.cit.arnejo.dormshare.auth

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREFS_NAME = "dormshare_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_LAST_NAME = "last_name"
    private const val KEY_ROLE = "role"
    private const val KEY_SELECTED_GROUP_ID = "selected_group_id"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(
        context: Context,
        token: String,
        userId: Long,
        email: String,
        firstName: String,
        lastName: String,
        role: String
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_TOKEN, token)
            putLong(KEY_USER_ID, userId)
            putString(KEY_EMAIL, email)
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    fun getToken(context: Context): String? = getPrefs(context).getString(KEY_TOKEN, null)
    fun getUserId(context: Context): Long = getPrefs(context).getLong(KEY_USER_ID, -1L)
    fun getEmail(context: Context): String? = getPrefs(context).getString(KEY_EMAIL, null)
    fun getFirstName(context: Context): String? = getPrefs(context).getString(KEY_FIRST_NAME, null)
    fun getLastName(context: Context): String? = getPrefs(context).getString(KEY_LAST_NAME, null)
    fun getUserName(context: Context): String {
        val first = getFirstName(context) ?: ""
        val last = getLastName(context) ?: ""
        return "$first $last".trim().ifEmpty { "User" }
    }
    fun getRole(context: Context): String? = getPrefs(context).getString(KEY_ROLE, null)

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean =
        !getToken(context).isNullOrEmpty()

    fun setSelectedGroupId(context: Context, groupId: Long) {
        getPrefs(context).edit().putLong(KEY_SELECTED_GROUP_ID, groupId).apply()
    }

    fun getSelectedGroupId(context: Context): Long =
        getPrefs(context).getLong(KEY_SELECTED_GROUP_ID, -1L)

    fun clearSelectedGroupId(context: Context) {
        getPrefs(context).edit().remove(KEY_SELECTED_GROUP_ID).apply()
    }
}
