package com.ferrero.inandout.archcomp

import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun <T> Fragment.getNavigationResult(key: String = "result") =
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)

fun <T> Fragment.setNavigationResult(result: T, key: String = "result") =
        findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)

fun <T> Fragment.removeNavigationResult(key: String = "result") =
        findNavController().currentBackStackEntry?.savedStateHandle?.remove<T>(key)

fun <T, U> List<T>.intersection(uList: List<U>, filterPredicate : (T, U) -> Boolean): List<T> = filter { m -> uList.any { filterPredicate(m, it)} }

