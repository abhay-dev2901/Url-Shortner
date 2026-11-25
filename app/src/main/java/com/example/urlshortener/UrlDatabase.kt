package com.example.urlshortener

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UrlDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE $TABLE_URLS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LONG_URL TEXT NOT NULL,
                $COLUMN_SHORT_URL TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) onCreate(database)
    }

    fun save(longUrl: String, shortUrl: String): Int {
        val values = ContentValues().apply {
            put(COLUMN_LONG_URL, longUrl)
            put(COLUMN_SHORT_URL, shortUrl)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        writableDatabase.insertOrThrow(TABLE_URLS, null, values)
        return count()
    }

    fun count(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_URLS", null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private companion object {
        const val DATABASE_NAME = "shortened_urls.db"
        const val DATABASE_VERSION = 1
        const val TABLE_URLS = "shortened_urls"
        const val COLUMN_ID = "id"
        const val COLUMN_LONG_URL = "long_url"
        const val COLUMN_SHORT_URL = "short_url"
        const val COLUMN_CREATED_AT = "created_at"
    }
}
