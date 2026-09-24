package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TranscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY createdAt DESC")
    fun getAllTranscriptions(): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    fun getTranscriptionById(id: Long): Flow<TranscriptionEntity?>

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    suspend fun getTranscriptionByIdOnce(id: Long): TranscriptionEntity?

    @Query("SELECT * FROM transcriptions WHERE fullText LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchTranscriptions(query: String): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavorites(): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<TranscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcription: TranscriptionEntity): Long

    @Update
    suspend fun update(transcription: TranscriptionEntity)

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transcriptions SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE transcriptions SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE transcriptions SET fullText = :text, segmentsJson = :segmentsJson WHERE id = :id")
    suspend fun updateContent(id: Long, text: String, segmentsJson: String)

    @Query("UPDATE transcriptions SET summary = :summary, actionItemsJson = :actionItemsJson WHERE id = :id")
    suspend fun updateSummary(id: Long, summary: String, actionItemsJson: String)
}
