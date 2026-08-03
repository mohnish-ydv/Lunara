package com.mohnishraj.lunara.data.cache

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.mohnishraj.lunara.domain.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "profile_cache")
data class ProfileCacheEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarSeed: Int,
    val shareCode: String,
    val isDiscoverable: Boolean,
    val allowRequests: Boolean,
    val cachedAt: Long,
)

@Dao
interface ProfileCacheDao {
    @Query("SELECT * FROM profile_cache WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<ProfileCacheEntity?>

    @Query("SELECT * FROM profile_cache WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ProfileCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileCacheEntity)

    @Query("DELETE FROM profile_cache")
    suspend fun clear()
}

@Database(entities = [ProfileCacheEntity::class], version = 2, exportSchema = true)
abstract class LunaraDatabase : RoomDatabase() {
    abstract fun profileCacheDao(): ProfileCacheDao
}

fun ProfileCacheEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    email = email,
    username = username,
    displayName = displayName,
    bio = bio,
    avatarSeed = avatarSeed,
    shareCode = shareCode,
    isDiscoverable = isDiscoverable,
    allowRequests = allowRequests,
)

fun UserProfile.toCache(): ProfileCacheEntity = ProfileCacheEntity(
    id = id,
    email = email,
    username = username,
    displayName = displayName,
    bio = bio,
    avatarSeed = avatarSeed,
    shareCode = shareCode,
    isDiscoverable = isDiscoverable,
    allowRequests = allowRequests,
    cachedAt = System.currentTimeMillis(),
)

fun ProfileCacheDao.observeDomain(id: String): Flow<UserProfile?> = observe(id).map { it?.toDomain() }
