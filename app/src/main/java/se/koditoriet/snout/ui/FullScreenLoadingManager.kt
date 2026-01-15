package se.koditoriet.snout.ui

import androidx.compose.runtime.mutableStateOf

object FullScreenLoadingManager {
    val isLoading = mutableStateOf(false)

    fun showLoader() {
        isLoading.value = true
    }

    fun hideLoader() {
        isLoading.value = false
    }
}