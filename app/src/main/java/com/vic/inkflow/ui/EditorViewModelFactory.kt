package com.vic.inkflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vic.inkflow.data.AppDatabase

class EditorViewModelFactory(private val db: AppDatabase, private val documentUri: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(db, documentUri) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}