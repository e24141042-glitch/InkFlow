package com.vic.inkflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.vic.inkflow.data.AppDatabase
import com.vic.inkflow.ui.InkLayerApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PDFBoxResourceLoader.init(applicationContext)
        val db = AppDatabase.getDatabase(this)
        setContent {
            InkLayerApp(db = db)
        }
    }
}