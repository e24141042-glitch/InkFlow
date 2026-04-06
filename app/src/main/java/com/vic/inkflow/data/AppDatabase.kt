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
// v8: Added document_preferences table for per-document editor setting overrides.
// v10: Added highlighterColorArgb column to document_preferences.
// v12: Added pageBackground column to document_preferences.
// v13: Added paperWidthPt / paperHeightPt columns to document_preferences.
// v14: Added per-tool pen/highlighter stroke width columns to document_preferences.
// v15: Added bookmarks table.
// v18: Rebuilt strokes table to remove document foreign key/index.
// v19: Added quickSwipeEraserEnabled to document_preferences.
@Database(
    entities = [StrokeEntity::class, PointEntity::class, DocumentEntity::class, FolderEntity::class,
                TextAnnotationEntity::class, ImageAnnotationEntity::class,
                DocumentPreferenceEntity::class, BookmarkEntity::class],
    version = 19
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun strokeDao(): StrokeDao
    abstract fun documentDao(): DocumentDao
    abstract fun folderDao(): FolderDao
    abstract fun textAnnotationDao(): TextAnnotationDao
    abstract fun imageAnnotationDao(): ImageAnnotationDao
    abstract fun documentPreferenceDao(): DocumentPreferenceDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v4: Added isHighlighter field to StrokeEntity.
                db.execSQL("ALTER TABLE strokes ADD COLUMN isHighlighter INTEGER NOT NULL DEFAULT 0")
            }
        }

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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS document_preferences (
                        documentUri TEXT NOT NULL PRIMARY KEY,
                        tool TEXT,
                        colorArgb INTEGER,
                        strokeWidth REAL,
                        shapeSubType TEXT,
                        stylusOnlyMode INTEGER,
                        recentColorsCsv TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE document_preferences ADD COLUMN inputMode TEXT")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE document_preferences ADD COLUMN highlighterColorArgb INTEGER")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE points ADD COLUMN width REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE document_preferences ADD COLUMN pageBackground TEXT")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE document_preferences ADD COLUMN paperWidthPt REAL")
                db.execSQL("ALTER TABLE document_preferences ADD COLUMN paperHeightPt REAL")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE document_preferences ADD COLUMN penStrokeWidth REAL")
                db.execSQL("ALTER TABLE document_preferences ADD COLUMN highlighterStrokeWidth REAL")
                db.execSQL(
                    "UPDATE document_preferences SET penStrokeWidth = COALESCE(strokeWidth, 4.0) WHERE penStrokeWidth IS NULL"
                )
                db.execSQL(
                    "UPDATE document_preferences SET highlighterStrokeWidth = CASE WHEN strokeWidth IS NULL OR strokeWidth < 8.0 THEN 8.0 ELSE strokeWidth END WHERE highlighterStrokeWidth IS NULL"
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        documentUri TEXT NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        PRIMARY KEY(documentUri, pageIndex)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE documents ADD COLUMN folderName TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS folders (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        parentFolderId TEXT DEFAULT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(parentFolderId) REFERENCES folders(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_folders_parentFolderId ON folders(parentFolderId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_folders_name_parentFolderId ON folders(name, parentFolderId)"
                )

                db.execSQL(
                    """
                    INSERT INTO folders(id, name, parentFolderId, sortOrder, createdAt, updatedAt)
                    SELECT lower(hex(randomblob(16))), trim(folderName), NULL, 0,
                           CAST(strftime('%s','now') AS INTEGER) * 1000,
                           CAST(strftime('%s','now') AS INTEGER) * 1000
                    FROM documents
                    WHERE folderName IS NOT NULL AND trim(folderName) != ''
                    GROUP BY trim(folderName)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS documents_new (
                        uri TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        lastOpenedAt INTEGER NOT NULL,
                        lastPageIndex INTEGER NOT NULL DEFAULT 0,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        folderId TEXT DEFAULT NULL,
                        FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO documents_new(uri, displayName, lastOpenedAt, lastPageIndex, isFavorite, folderId)
                    SELECT d.uri, d.displayName, d.lastOpenedAt, d.lastPageIndex, d.isFavorite, f.id
                    FROM documents d
                    LEFT JOIN folders f
                      ON f.name = trim(d.folderName)
                     AND f.parentFolderId IS NULL
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE documents")
                db.execSQL("ALTER TABLE documents_new RENAME TO documents")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_folderId ON documents(folderId)")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rebuild tables to align with current entity schema (no legacy FK/index constraints).
                db.execSQL("PRAGMA foreign_keys=OFF")
                db.beginTransaction()
                try {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS strokes_new (
                            id TEXT NOT NULL PRIMARY KEY,
                            documentUri TEXT NOT NULL,
                            pageIndex INTEGER NOT NULL,
                            color INTEGER NOT NULL,
                            strokeWidth REAL NOT NULL,
                            boundsLeft REAL NOT NULL,
                            boundsTop REAL NOT NULL,
                            boundsRight REAL NOT NULL,
                            boundsBottom REAL NOT NULL,
                            isHighlighter INTEGER NOT NULL,
                            shapeType TEXT
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        INSERT INTO strokes_new(
                            id,
                            documentUri,
                            pageIndex,
                            color,
                            strokeWidth,
                            boundsLeft,
                            boundsTop,
                            boundsRight,
                            boundsBottom,
                            isHighlighter,
                            shapeType
                        )
                        SELECT
                            id,
                            documentUri,
                            pageIndex,
                            color,
                            strokeWidth,
                            boundsLeft,
                            boundsTop,
                            boundsRight,
                            boundsBottom,
                            isHighlighter,
                            shapeType
                        FROM strokes
                        """.trimIndent()
                    )

                    db.execSQL("DROP TABLE strokes")
                    db.execSQL("ALTER TABLE strokes_new RENAME TO strokes")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS text_annotations_new (
                            id TEXT NOT NULL PRIMARY KEY,
                            documentUri TEXT NOT NULL,
                            pageIndex INTEGER NOT NULL,
                            text TEXT NOT NULL,
                            modelX REAL NOT NULL,
                            modelY REAL NOT NULL,
                            fontSize REAL NOT NULL,
                            colorArgb INTEGER NOT NULL,
                            isStamp INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        INSERT INTO text_annotations_new(
                            id,
                            documentUri,
                            pageIndex,
                            text,
                            modelX,
                            modelY,
                            fontSize,
                            colorArgb,
                            isStamp
                        )
                        SELECT
                            id,
                            documentUri,
                            pageIndex,
                            text,
                            modelX,
                            modelY,
                            fontSize,
                            colorArgb,
                            isStamp
                        FROM text_annotations
                        """.trimIndent()
                    )

                    db.execSQL("DROP TABLE text_annotations")
                    db.execSQL("ALTER TABLE text_annotations_new RENAME TO text_annotations")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS image_annotations_new (
                            id TEXT NOT NULL PRIMARY KEY,
                            documentUri TEXT NOT NULL,
                            pageIndex INTEGER NOT NULL,
                            uri TEXT NOT NULL,
                            modelX REAL NOT NULL,
                            modelY REAL NOT NULL,
                            modelWidth REAL NOT NULL,
                            modelHeight REAL NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        INSERT INTO image_annotations_new(
                            id,
                            documentUri,
                            pageIndex,
                            uri,
                            modelX,
                            modelY,
                            modelWidth,
                            modelHeight
                        )
                        SELECT
                            id,
                            documentUri,
                            pageIndex,
                            uri,
                            modelX,
                            modelY,
                            modelWidth,
                            modelHeight
                        FROM image_annotations
                        """.trimIndent()
                    )

                    db.execSQL("DROP TABLE image_annotations")
                    db.execSQL("ALTER TABLE image_annotations_new RENAME TO image_annotations")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS document_preferences_new (
                            documentUri TEXT NOT NULL PRIMARY KEY,
                            tool TEXT,
                            colorArgb INTEGER,
                            strokeWidth REAL,
                            penStrokeWidth REAL,
                            highlighterStrokeWidth REAL,
                            shapeSubType TEXT,
                            stylusOnlyMode INTEGER,
                            inputMode TEXT,
                            recentColorsCsv TEXT,
                            highlighterColorArgb INTEGER,
                            pageBackground TEXT,
                            paperWidthPt REAL,
                            paperHeightPt REAL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        INSERT INTO document_preferences_new(
                            documentUri,
                            tool,
                            colorArgb,
                            strokeWidth,
                            penStrokeWidth,
                            highlighterStrokeWidth,
                            shapeSubType,
                            stylusOnlyMode,
                            inputMode,
                            recentColorsCsv,
                            highlighterColorArgb,
                            pageBackground,
                            paperWidthPt,
                            paperHeightPt
                        )
                        SELECT
                            documentUri,
                            tool,
                            colorArgb,
                            strokeWidth,
                            penStrokeWidth,
                            highlighterStrokeWidth,
                            shapeSubType,
                            stylusOnlyMode,
                            inputMode,
                            recentColorsCsv,
                            highlighterColorArgb,
                            pageBackground,
                            paperWidthPt,
                            paperHeightPt
                        FROM document_preferences
                        """.trimIndent()
                    )

                    db.execSQL("DROP TABLE document_preferences")
                    db.execSQL("ALTER TABLE document_preferences_new RENAME TO document_preferences")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS bookmarks_new (
                            documentUri TEXT NOT NULL,
                            pageIndex INTEGER NOT NULL,
                            PRIMARY KEY(documentUri, pageIndex)
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        INSERT INTO bookmarks_new(documentUri, pageIndex)
                        SELECT documentUri, pageIndex
                        FROM bookmarks
                        """.trimIndent()
                    )

                    db.execSQL("DROP TABLE bookmarks")
                    db.execSQL("ALTER TABLE bookmarks_new RENAME TO bookmarks")

                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE document_preferences ADD COLUMN quickSwipeEraserEnabled INTEGER")
                db.execSQL(
                    "UPDATE document_preferences SET quickSwipeEraserEnabled = 0 WHERE quickSwipeEraserEnabled IS NULL"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ink_layer_database"
                )
                .addMigrations(
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19
                )
                // Only allow destructive migration on downgrade (e.g. user reverts to an
                // older APK). Unknown *upgrade* paths surface as a hard crash rather than
                // silently wiping all user data.
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}