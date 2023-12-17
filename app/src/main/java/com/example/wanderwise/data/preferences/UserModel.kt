package com.example.wanderwise.data.preferences

import android.net.Uri

data class UserModel(
    val name: String,
    val token: String,
    val email: String,
    val isLogin: Boolean = false
)