package com.ferrero.inandout.viewmodel

import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ferrero.inandout.viewmodel.home.HomeViewModel


@Suppress("UNCHECKED_CAST")
class EntityViewModelFactory(private vararg val args: Any) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>) = with(modelClass) {
        when {
            modelClass.isAssignableFrom(MainBusinnesViewModel::class.java) -> {
                return MainBusinnesViewModel(args[0] as Context, args[1] as Application) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                return HomeViewModel(args[0] as Fragment) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}