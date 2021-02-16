package com.example.baenger

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize data class UserDetails(
        val token: String?,
        val id: String?,
        val username: String?,
        val playlistID: String?
): Parcelable
