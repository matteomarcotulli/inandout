package com.ferrero.inandout.archcomp

import android.content.Context
import android.net.ConnectivityManager

object Utils {

    fun checkOnlineDevice(context: Context): Boolean {
        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = connectivityManager.activeNetworkInfo
        return netInfo?.isConnected ?: false
    }
}