package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "districts")
data class DistrictEntity(
  @PrimaryKey val districtId: String,
  val name: String,
  val nameTe: String,
  val headquarters: String,
  val latitude: Double,
  val longitude: Double,
  val mapX: Float,
  val mapY: Float,
  val mandalCount: Int,
  val gpCount: Int,
)

@Entity(tableName = "mandals")
data class MandalEntity(
  @PrimaryKey val mandalId: String,
  val districtId: String,
  val name: String,
  val nameTe: String,
)

@Entity(tableName = "villages")
data class VillageEntity(
  @PrimaryKey val villageId: String,
  val mandalId: String,
  val districtId: String,
  val name: String,
  val gramPanchayatName: String,
  val wardCount: Int,
)

object TelanganaGeoSeed {
  val districts: List<DistrictEntity> =
    listOf(
      DistrictEntity("adilabad", "Adilabad", "ఆదిలాబాద్", "Adilabad", 19.6641, 78.5320, 0.34f, 0.09f, 18, 468),
      DistrictEntity("bhadradri_kothagudem", "Bhadradri Kothagudem", "భద్రాద్రి కొత్తగూడెం", "Kothagudem", 17.5500, 80.6197, 0.85f, 0.56f, 23, 481),
      DistrictEntity("hanumakonda", "Hanumakonda", "హనుమకొండ", "Hanumakonda", 18.0000, 79.5500, 0.57f, 0.42f, 14, 210),
      DistrictEntity("hyderabad", "Hyderabad", "హైదరాబాద్", "Hyderabad", 17.3850, 78.4867, 0.36f, 0.58f, 16, 150),
      DistrictEntity("jagtial", "Jagtial", "జగిత్యాల", "Jagtial", 18.7895, 78.9120, 0.43f, 0.25f, 18, 380),
      DistrictEntity("jangaon", "Jangaon", "జనగామ", "Jangaon", 17.7267, 79.1520, 0.49f, 0.50f, 12, 281),
      DistrictEntity("jayashankar_bhupalpally", "Jayashankar Bhupalpally", "జయశంకర్ భూపాలపల్లి", "Bhupalpally", 18.4357, 79.8655, 0.66f, 0.33f, 11, 241),
      DistrictEntity("jogulamba_gadwal", "Jogulamba Gadwal", "జోగులాంబ గద్వాల", "Gadwal", 16.2333, 77.8000, 0.23f, 0.89f, 12, 255),
      DistrictEntity("kamareddy", "Kamareddy", "కామారెడ్డి", "Kamareddy", 18.3204, 78.3422, 0.29f, 0.35f, 22, 526),
      DistrictEntity("karimnagar", "Karimnagar", "కరీంనగర్", "Karimnagar", 18.4386, 79.1288, 0.49f, 0.33f, 16, 313),
      DistrictEntity("khammam", "Khammam", "ఖమ్మం", "Khammam", 17.2473, 80.1514, 0.74f, 0.64f, 21, 589),
      DistrictEntity("kumuram_bheem", "Kumuram Bheem Asifabad", "కొమురంభీం ఆసిఫాబాద్", "Asifabad", 19.3586, 79.2869, 0.50f, 0.13f, 15, 335),
      DistrictEntity("mahabubabad", "Mahabubabad", "మహబూబాబాద్", "Mahabubabad", 17.6022, 80.0020, 0.69f, 0.53f, 16, 461),
      DistrictEntity("mahabubnagar", "Mahabubnagar", "మహబూబ్‌నగర్", "Mahabubnagar", 16.7488, 78.0035, 0.27f, 0.75f, 15, 441),
      DistrictEntity("mancherial", "Mancherial", "మంచిర్యాల", "Mancherial", 18.8756, 79.4591, 0.56f, 0.21f, 18, 311),
      DistrictEntity("medak", "Medak", "మెదక్", "Medak", 18.0453, 78.2608, 0.30f, 0.43f, 21, 469),
      DistrictEntity("medchal_malkajgiri", "Medchal-Malkajgiri", "మేడ్చల్ మల్కాజిగిరి", "Medchal", 17.6290, 78.4814, 0.38f, 0.52f, 15, 161),
      DistrictEntity("mulugu", "Mulugu", "ములుగు", "Mulugu", 18.1924, 79.9410, 0.72f, 0.41f, 9, 174),
      DistrictEntity("nagarkurnool", "Nagarkurnool", "నాగర్‌కర్నూల్", "Nagarkurnool", 16.4820, 78.3208, 0.36f, 0.83f, 20, 461),
      DistrictEntity("nalgonda", "Nalgonda", "నల్గొండ", "Nalgonda", 17.0575, 79.2684, 0.52f, 0.71f, 31, 844),
      DistrictEntity("narayanpet", "Narayanpet", "నారాయణపేట", "Narayanpet", 16.7445, 77.4963, 0.16f, 0.76f, 11, 280),
      DistrictEntity("nirmal", "Nirmal", "నిర్మల్", "Nirmal", 19.0964, 78.3430, 0.31f, 0.18f, 19, 396),
      DistrictEntity("nizamabad", "Nizamabad", "నిజామాబాద్", "Nizamabad", 18.6725, 78.0941, 0.24f, 0.26f, 29, 530),
      DistrictEntity("peddapalli", "Peddapalli", "పెద్దపల్లి", "Peddapalli", 18.6152, 79.3738, 0.54f, 0.28f, 14, 266),
      DistrictEntity("rajanna_sircilla", "Rajanna Sircilla", "రాజన్న సిరిసిల్ల", "Sircilla", 18.3872, 78.8087, 0.40f, 0.34f, 13, 255),
      DistrictEntity("rangareddy", "Rangareddy", "రంగారెడ్డి", "Rajendranagar", 17.2403, 78.4294, 0.34f, 0.65f, 27, 558),
      DistrictEntity("sangareddy", "Sangareddy", "సంగారెడ్డి", "Sangareddy", 17.6194, 78.0816, 0.22f, 0.53f, 27, 647),
      DistrictEntity("siddipet", "Siddipet", "సిద్దిపేట", "Siddipet", 18.1018, 78.8520, 0.43f, 0.42f, 24, 499),
      DistrictEntity("suryapet", "Suryapet", "సూర్యాపేట", "Suryapet", 17.1415, 79.6236, 0.63f, 0.68f, 23, 475),
      DistrictEntity("vikarabad", "Vikarabad", "వికారాబాద్", "Vikarabad", 17.3364, 77.9048, 0.20f, 0.63f, 19, 566),
      DistrictEntity("wanaparthy", "Wanaparthy", "వనపర్తి", "Wanaparthy", 16.3610, 78.0628, 0.28f, 0.84f, 14, 255),
      DistrictEntity("warangal", "Warangal", "వరంగల్", "Warangal", 17.9689, 79.5941, 0.60f, 0.47f, 13, 323),
      DistrictEntity("yadadri_bhuvanagiri", "Yadadri Bhuvanagiri", "యాదాద్రి భువనగిరి", "Bhongir", 17.5105, 78.8888, 0.47f, 0.58f, 17, 421),
    )

  fun defaultMandalsForDistrict(district: DistrictEntity): List<MandalEntity> {
    val specific =
      when (district.districtId) {
        "warangal" ->
          listOf(
            MandalEntity("wardhannapet", "warangal", "Wardhannapet", "వర్ధన్నపేట"),
            MandalEntity("narsampet", "warangal", "Narsampet", "నర్సంపేట"),
            MandalEntity("parvathagiri", "warangal", "Parvathagiri", "పర్వతగిరి"),
          )
        "khammam" ->
          listOf(
            MandalEntity("wyra", "khammam", "Wyra", "వైరా"),
            MandalEntity("madhira", "khammam", "Madhira", "మధిర"),
            MandalEntity("sathupalli", "khammam", "Sathupalli", "సత్తుపల్లి"),
          )
        "nalgonda" ->
          listOf(
            MandalEntity("miryalaguda", "nalgonda", "Miryalaguda", "మిర్యాలగూడ"),
            MandalEntity("devarakonda", "nalgonda", "Devarakonda", "దేవరకొండ"),
            MandalEntity("nakrekal", "nalgonda", "Nakrekal", "నక్రేకల్"),
          )
        "siddipet" ->
          listOf(
            MandalEntity("gajwel", "siddipet", "Gajwel", "గజ్వేల్"),
            MandalEntity("husnabad", "siddipet", "Husnabad", "హుస్నాబాద్"),
            MandalEntity("dubbak", "siddipet", "Dubbak", "దుబ్బాక"),
          )
        "karimnagar" ->
          listOf(
            MandalEntity("huzurabad", "karimnagar", "Huzurabad", "హుజూరాబాద్"),
            MandalEntity("choppadandi", "karimnagar", "Choppadandi", "చొప్పదండి"),
            MandalEntity("manakondur", "karimnagar", "Manakondur", "మానకొండూరు"),
          )
        "nizamabad" ->
          listOf(
            MandalEntity("armoor", "nizamabad", "Armoor", "ఆర్మూర్"),
            MandalEntity("bodhan", "nizamabad", "Bodhan", "బోధన్"),
            MandalEntity("balkonda", "nizamabad", "Balkonda", "బాల్కొండ"),
          )
        "mahabubnagar" ->
          listOf(
            MandalEntity("jadcherla", "mahabubnagar", "Jadcherla", "జడ్చర్ల"),
            MandalEntity("devarkadra", "mahabubnagar", "Devarkadra", "దేవరకద్ర"),
            MandalEntity("bhootpur", "mahabubnagar", "Bhootpur", "భూత్పూర్"),
          )
        else ->
          listOf(
            MandalEntity("${district.districtId}_central", district.districtId, "${district.headquarters} Rural", "${district.nameTe} గ్రామీణ"),
            MandalEntity("${district.districtId}_north", district.districtId, "${district.headquarters} North", "${district.nameTe} ఉత్తర"),
            MandalEntity("${district.districtId}_south", district.districtId, "${district.headquarters} South", "${district.nameTe} దక్షిణ"),
          )
      }
    return specific
  }

  fun defaultVillagesForMandal(mandal: MandalEntity): List<VillageEntity> {
    return when (mandal.mandalId) {
      "wardhannapet" ->
        listOf(
          VillageEntity("ilanda", "wardhannapet", "warangal", "Ilanda", "Ilanda GP", 12),
          VillageEntity("dammannapet", "wardhannapet", "warangal", "Dammannapet", "Dammannapet GP", 10),
          VillageEntity("kothapally_wgl", "wardhannapet", "warangal", "Kothapally", "Kothapally GP", 8),
        )
      "wyra" ->
        listOf(
          VillageEntity("sn_puram", "wyra", "khammam", "S.N. Puram", "S.N. Puram GP", 10),
          VillageEntity("somavaram", "wyra", "khammam", "Somavaram", "Somavaram GP", 8),
          VillageEntity("rebbavaram", "wyra", "khammam", "Rebbavaram", "Rebbavaram GP", 10),
        )
      else ->
        listOf(
          VillageEntity("${mandal.mandalId}_gp1", mandal.mandalId, mandal.districtId, "${mandal.name} Gram Panchayat", "${mandal.name} GP", 10),
          VillageEntity("${mandal.mandalId}_gp2", mandal.mandalId, mandal.districtId, "Venkatapur", "Venkatapur GP", 8),
          VillageEntity("${mandal.mandalId}_gp3", mandal.mandalId, mandal.districtId, "Ramachandrapuram", "Ramachandrapuram GP", 12),
        )
    }
  }
}
