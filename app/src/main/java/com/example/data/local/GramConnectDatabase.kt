package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TelanganaGeoDao {
  @Query("SELECT * FROM districts ORDER BY name ASC")
  fun observeDistricts(): Flow<List<DistrictEntity>>

  @Query("SELECT COUNT(*) FROM districts")
  suspend fun getDistrictCount(): Int

  @Query("SELECT * FROM mandals WHERE districtId = :districtId ORDER BY name ASC")
  fun observeMandals(districtId: String): Flow<List<MandalEntity>>

  @Query("SELECT * FROM villages WHERE mandalId = :mandalId ORDER BY name ASC")
  fun observeVillages(mandalId: String): Flow<List<VillageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDistricts(items: List<DistrictEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMandals(items: List<MandalEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVillages(items: List<VillageEntity>)
}

@Database(
  entities = [DistrictEntity::class, MandalEntity::class, VillageEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class GramConnectDatabase : RoomDatabase() {
  abstract fun geoDao(): TelanganaGeoDao

  companion object {
    @Volatile private var INSTANCE: GramConnectDatabase? = null

    fun getInstance(context: Context): GramConnectDatabase {
      return INSTANCE
        ?: synchronized(this) {
          val instance =
            Room.databaseBuilder(
                context.applicationContext,
                GramConnectDatabase::class.java,
                "gramconnect_telangana.db",
              )
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}

class LocalGeoRepository(private val dao: TelanganaGeoDao) {
  val districts: Flow<List<DistrictEntity>> = dao.observeDistricts()

  fun mandalsFor(districtId: String): Flow<List<MandalEntity>> = dao.observeMandals(districtId)

  fun villagesFor(mandalId: String): Flow<List<VillageEntity>> = dao.observeVillages(mandalId)

  suspend fun ensureSeeded() {
    if (dao.getDistrictCount() < 33) {
      val districts = TelanganaGeoSeed.districts
      val mandals = districts.flatMap { TelanganaGeoSeed.defaultMandalsForDistrict(it) }
      val villages = mandals.flatMap { TelanganaGeoSeed.defaultVillagesForMandal(it) }
      dao.insertDistricts(districts)
      dao.insertMandals(mandals)
      dao.insertVillages(villages)
    }
  }

  suspend fun addCustomMandalOrVillage(
    districtId: String,
    mandalName: String,
    villageName: String,
  ) {
    val mandalSlug = "${districtId}_${mandalName.lowercase().replace(Regex("[^a-z0-9]"), "_")}"
    val mandal = MandalEntity(mandalSlug, districtId, mandalName.trim(), mandalName.trim())
    val villageSlug = "${mandalSlug}_${villageName.lowercase().replace(Regex("[^a-z0-9]"), "_")}"
    val village = VillageEntity(villageSlug, mandalSlug, districtId, villageName.trim(), "${villageName.trim()} GP", 10)
    dao.insertMandals(listOf(mandal))
    dao.insertVillages(listOf(village))
  }
}
