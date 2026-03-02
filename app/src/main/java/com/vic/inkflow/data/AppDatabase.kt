package com.vic.inkflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
// [Refactored] Added DocumentEntity; bumped version to 3.
// v4: Added isHighlighter field to StrokeEntity.
// v5: Added lastPageIndex to DocumentEntity.
// v6: Added documentUri to StrokeEntity to scope strokes per document.
// v7: Added shapeType to StrokeEntity; added text_annotations & image_annotations tables.
@Database(
    entities = [StrokeEntity::class, PointEntity::class, DocumentEntity::class,
                TextAnnotationEntity::class, ImageAnnotationEntity::class],
    version = 7
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun strokeDao(): StrokeDao
    abstract fun documentDao(): DocumentDao
    abstract fun textAnnotationDao(): TextAnnotationDao
    abstract fun imageAnnotationDao(): ImageAnnotationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN lastPageIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add documentUri column; existing rows get empty string (legacy data, no owner).
                db.execSQL("ALTER TABLE strokes ADD COLUMN documentUri TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE strokes ADD COLUMN shapeType TEXT")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS text_annotations (
                        id TEXT NOT NULL PRIMARY KEY,
                        documentUri TEXT NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        modelX REAL NOT NULL,
                        modelY REAL NOT NULL,
                        fontSize REAL NOT NULL DEFAULT 16.0,
                        colorArgb INTEGER NOT NULL,
                        isStamp INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS image_annotations (
                        id TEXT NOT NULL PRIMARY KEY,
                        documentUri TEXT NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        uri TEXT NOT NULL,
                        modelX REAL NOT NULL,
                        modelY REAL NOT NULL,
                        modelWidth REAL NOT NULL,
                        modelHeight REAL NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ink_layer_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}