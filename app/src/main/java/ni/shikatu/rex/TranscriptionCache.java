package ni.shikatu.rex;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

/**
 * SQLite cache for local Whisper transcriptions.
 * Uses file ID as key and stores transcription text + detected language.
 */
public class TranscriptionCache extends SQLiteOpenHelper {

  private static final String DB_NAME = "whisper_cache.db";
  private static final int DB_VERSION = 1;

  private static final String TABLE_NAME = "transcriptions";
  private static final String COL_FILE_ID = "file_id";
  private static final String COL_TEXT = "text";
  private static final String COL_LANGUAGE = "language";
  private static final String COL_TIMESTAMP = "timestamp";

  private static final int MAX_CACHE_SIZE = 500;

  private static TranscriptionCache instance;

  public static class CachedTranscription {
    public final String text;
    public final String language;
    public final long timestamp;

    public CachedTranscription(String text, @Nullable String language, long timestamp) {
      this.text = text;
      this.language = language;
      this.timestamp = timestamp;
    }
  }

  private TranscriptionCache(Context context) {
    super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
  }

  public static synchronized TranscriptionCache getInstance(Context context) {
    if (instance == null) {
      instance = new TranscriptionCache(context);
    }
    return instance;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
      COL_FILE_ID + " INTEGER PRIMARY KEY, " +
      COL_TEXT + " TEXT NOT NULL, " +
      COL_LANGUAGE + " TEXT, " +
      COL_TIMESTAMP + " INTEGER NOT NULL)");

    db.execSQL("CREATE INDEX idx_timestamp ON " + TABLE_NAME + " (" + COL_TIMESTAMP + ")");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    onCreate(db);
  }

  /**
   * Get cached transcription for a file.
   * @param fileId TDLib file ID
   * @return cached transcription or null if not found
   */
  @Nullable
  public CachedTranscription get(int fileId) {
    SQLiteDatabase db = getReadableDatabase();
    try (Cursor cursor = db.query(TABLE_NAME,
      new String[]{COL_TEXT, COL_LANGUAGE, COL_TIMESTAMP},
      COL_FILE_ID + " = ?",
      new String[]{String.valueOf(fileId)},
      null, null, null)) {

      if (cursor.moveToFirst()) {
        return new CachedTranscription(
          cursor.getString(0),
          cursor.getString(1),
          cursor.getLong(2)
        );
      }
    }
    return null;
  }

  /**
   * Cache a transcription result.
   * @param fileId TDLib file ID
   * @param text transcription text
   * @param language detected language (can be null)
   */
  public void put(int fileId, String text, @Nullable String language) {
    if (text == null || text.isEmpty()) {
      return;
    }

    SQLiteDatabase db = getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(COL_FILE_ID, fileId);
    values.put(COL_TEXT, text);
    values.put(COL_LANGUAGE, language);
    values.put(COL_TIMESTAMP, System.currentTimeMillis());

    db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

    // Cleanup if needed
    cleanupIfNeeded(db);
  }

  /**
   * Check if transcription is cached.
   * @param fileId TDLib file ID
   * @return true if cached
   */
  public boolean has(int fileId) {
    SQLiteDatabase db = getReadableDatabase();
    try (Cursor cursor = db.rawQuery(
      "SELECT 1 FROM " + TABLE_NAME + " WHERE " + COL_FILE_ID + " = ? LIMIT 1",
      new String[]{String.valueOf(fileId)})) {
      return cursor.moveToFirst();
    }
  }

  /**
   * Remove a cached transcription.
   * @param fileId TDLib file ID
   */
  public void remove(int fileId) {
    SQLiteDatabase db = getWritableDatabase();
    db.delete(TABLE_NAME, COL_FILE_ID + " = ?", new String[]{String.valueOf(fileId)});
  }

  /**
   * Clear all cached transcriptions.
   */
  public void clear() {
    SQLiteDatabase db = getWritableDatabase();
    db.delete(TABLE_NAME, null, null);
  }

  /**
   * Get cache size.
   */
  public int size() {
    SQLiteDatabase db = getReadableDatabase();
    try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null)) {
      if (cursor.moveToFirst()) {
        return cursor.getInt(0);
      }
    }
    return 0;
  }

  private void cleanupIfNeeded(SQLiteDatabase db) {
    try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null)) {
      if (cursor.moveToFirst() && cursor.getInt(0) > MAX_CACHE_SIZE) {
        // Delete oldest entries
        int toDelete = cursor.getInt(0) - MAX_CACHE_SIZE + 50;
        db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COL_FILE_ID + " IN (" +
          "SELECT " + COL_FILE_ID + " FROM " + TABLE_NAME +
          " ORDER BY " + COL_TIMESTAMP + " ASC LIMIT " + toDelete + ")");
      }
    }
  }
}
